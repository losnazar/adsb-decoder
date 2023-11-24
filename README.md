**Task Description: Processing and Decoding ADS-B Data**

This task is designed to evaluate proficiency in handling binary data, implementing multithreading, and understanding external APIs.

**Objective:**
The primary objective of this task is to process and decode raw Automatic Dependent Surveillance-Broadcast (ADS-B) data from aircraft. This involves three main tasks: reading raw binary data, decoding the ADS-B messages, and implementing multithreading to enhance performance.

**Raw Data Example:**
- Example of a raw ADS-B message: `8da83f6b9908e2a0880c17268156`
- In this example, the unique aircraft identifier (ICAO code) is `a83f6b`, which is embedded in the message: `8d**a83f6b**9908e2a0880c17268156`.

**Data Format:**
- In text files, ADS-B messages are represented as strings formatted in hexadecimal.
- In binary files, the actual ADS-B messages from aircraft are stored, with each message being 14 bytes in length.
- The binary file contains these messages consecutively, without any separators. Thus, to retrieve all raw messages, one should read the file in 14-byte chunks.
- Those messages are real data coming from 100+ receivers, so be aware that some messages could be duplicated due to being received by several receivers.

## Tasks:
### **Task 1. Reading Raw Binary Data:**
- Extract raw messages from the binary file by reading 14-byte segments sequentially.

### **Task 2. Decoding ADS-B Messages:**
- Utilize the lib1090 library to decode the raw ADS-B messages.
- Decoded information typically includes aircraft identity, position, altitude, and velocity.
- Repository for lib1090: [https://github.com/SeRoSystems/lib1090/](https://github.com/SeRoSystems/lib1090/).
- Example of usage of decoder: [https://github.com/openskynetwork/java-adsb/blob/master/src/main/java/org/opensky/example/ExampleDecoder.java](https://github.com/openskynetwork/java-adsb/blob/master/src/main/java/org/opensky/example/ExampleDecoder.java)
- Compile data elements into a unified structure using the ICAO code as the unique identifier for each aircraft.
  At a minimum, decode:
    - ICAO code (icao24)
    - Callsign (also called identity or flight number) [ADSB_IDENTIFICATION getIdentity](https://github.com/openskynetwork/java-adsb/blob/7d73726841d98357da360ea631bbe12d3def86cc/src/main/java/org/opensky/example/ExampleDecoder.java#L201)
    - Speed
    - Additional fields are optional and can be included at your discretion.
- Display the aggregated data as preferred: save to a file, print in the console, or store in a database.

### **Task 3. Implementing Multithreading:**
- Enhance the processing performance by implementing multithreading.
- This approach allows parallel processing of multiple ADS-B messages, improving overall efficiency.



## **The resulting code upload on github:**
Once your solution is ready and tested, you can upload it to a new GitHub repository.


## **Additional Resources:**
- Tool for decoding ADS-B frames: [http://jasonplayne.com:8080/](http://jasonplayne.com:8080/).

## **Contact for Clarification:**
- Should you require further clarification or additional details, please contact Artem Loiko via Telegram: @Artem_Loiko.

---

### **Additional Information About ADS-B Format:**
- ADS-B messages are 112 bits long and are used for transmitting various data such as aircraft identity, position, and other flight-related information.
- The message format is highly standardized for compatibility across different ADS-B systems.
- Understanding the structure of these messages is crucial for accurate decoding and interpretation.
- ADS-B basics [https://mode-s.org/decode/content/ads-b/1-basics.html](https://mode-s.org/decode/content/ads-b/1-basics.html)
