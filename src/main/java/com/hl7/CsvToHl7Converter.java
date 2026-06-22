package com.hl7;


import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.v24.message.ADT_A01;
import ca.uhn.hl7v2.model.v24.segment.MSH;
import ca.uhn.hl7v2.model.v24.segment.PID;
import ca.uhn.hl7v2.parser.Parser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Enterprise utility to convert CSV rows into standard HL7 v2.4 ADT^A01 messages.
 * Uses the HAPI (HL7 API) library for guaranteed structural compliance.
 */
@Service
public class CsvToHl7Converter {

    // Using a shared context as it is expensive to initialize repeatedly
    private static final HapiContext context = new DefaultHapiContext();
    private static final Parser parser = context.getPipeParser();

    /**
     * Reads the CSV file from the resources folder and converts each data row into an HL7 message.
     */
    public List<String> processCsvResource(String resourceName) throws Exception {
        // Load the file from the src/main/resources folder (classpath)
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);

        if (inputStream == null) {
            throw new IllegalArgumentException("CSV file not found in resources: " + resourceName);
        }

        return processStream(inputStream);
    }

    /**
     * Processes an uploaded CSV file from a REST endpoint.
     */
    public List<String> processCsvUpload(MultipartFile file) throws Exception {
        return processStream(file.getInputStream());
    }

    /**
     * Core logic to read the input stream and generate a list of HL7 messages.
     */
    private List<String> processStream(InputStream inputStream) throws Exception {
        List<String> hl7Messages = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; // Skip the header row
                    continue;
                }

                String[] columns = line.split(",");
                if (columns.length >= 5) {
                    // Extract data based on known CSV structure
                    String patientId = columns[0].trim();
                    String lastName = columns[1].trim();
                    String firstName = columns[2].trim();
                    String dob = columns[3].trim(); // Expected YYYYMMDD
                    String gender = columns[4].trim();

                    // Generate the HL7 Message
                    ADT_A01 hl7Message = createAdtA01Message(patientId, lastName, firstName, dob, gender);

                    // Serialize to pipe-delimited string format
                    String encodedMessage = parser.encode(hl7Message);

                    // Normalize line endings to standard \r segment terminators for HL7 (returned as string)
                    hl7Messages.add(encodedMessage);
                }
            }
        }
        return hl7Messages;
    }

    /**
     * Constructs a valid ADT^A01 message using HAPI object models.
     */
    private ADT_A01 createAdtA01Message(String patientId, String lastName, String firstName, String dob, String gender) throws Exception {
        ADT_A01 adt = new ADT_A01();

        // Initialize the message with standard MSH parameters (Message Type, Trigger Event, Processing ID)
        adt.initQuickstart("ADT", "A01", "P");

        // 1. Populate the MSH (Message Header) Segment
        MSH msh = adt.getMSH();
        msh.getSendingApplication().getNamespaceID().setValue("MySourceSystem");
        msh.getReceivingApplication().getNamespaceID().setValue("EnterpriseEHR");

        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        msh.getDateTimeOfMessage().getTimeOfAnEvent().setValue(timestamp);
        msh.getMessageControlID().setValue("MSG-" + System.currentTimeMillis());

        // 2. Populate the PID (Patient Identification) Segment
        PID pid = adt.getPID();

        // PID-3: Patient ID
        pid.getPatientIdentifierList(0).getID().setValue(patientId);

        // PID-5: Patient Name
        pid.getPatientName(0).getFamilyName().getSurname().setValue(lastName);
        pid.getPatientName(0).getGivenName().setValue(firstName);

        // PID-7: Date of Birth
        pid.getDateTimeOfBirth().getTimeOfAnEvent().setValue(dob);

        // PID-8: Gender (M/F/O/U)
        pid.getAdministrativeSex().setValue(gender);

        return adt;
    }
}
