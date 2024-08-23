
# Publish Message Microservice

## Overview

The `PublishMessageMicroservice` project is a Java-based microservice designed for handling message publishing to various queues and managing authorisation. It is structured using a modular architecture to ensure scalability, maintainability, and efficient processing of queue requests. This microservice is part of the Livestock Event Information Sharing Architecture (LEISA), facilitating efficient and standardised data exchange in the livestock industry.

## Related Projects

- [LEI Schema](https://github.com/mahirgamal/LEI-schema): Defines the standardised schema for livestock event information.
- [LEISA](https://github.com/mahirgamal/LEISA): The architecture framework for sharing livestock event information.
- [LEI2JSON](https://github.com/mahirgamal/LEI2JSON): A tool to convert LEI data into JSON format for easy processing.
- [AgriVet Treatment Grapher](https://github.com/mahirgamal/AgriVet-Treatment-Grapher): A Python-based tool designed to visualise treatment data for animals, helping veterinarians and researchers analyze treatment patterns and dosages.
- [Cattle Location Monitor](https://github.com/mahirgamal/Cattle-Location-Monitor): A system that monitors cattle location using GPS data to provide real-time insights into cattle movements and positioning.
- 
## Features

- **Queue Mapping**: Manage and map different types of requests to the appropriate queues.
- **Message Publishing**: Publish messages to designated queues with robust handling.
- **Authorisation**: Implement secure access control to manage who can publish messages.

## Architecture

The application is structured using a modular architecture:

1. **Function Layer (`com.function`)**: Contains utility functions and manages the mapping of queue requests.
2. **Domain Layer (`com.domain`)**: Responsible for publishing messages to queues and handling authorisation processes.

## Project Structure

```
/PublishMessageMicroservice
│
├── .git                      # Git configuration directory
├── .gitignore                # Git ignore file
├── host.json                 # Configuration file for hosting
├── local.settings.json       # Local environment settings file
├── pom.xml                   # Project Object Model file for Maven
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       ├── domain
│   │   │       │   ├── Authorisation.java        # Handles authorisation logic
│   │   │       │   └── PublishMessage.java       # Manages publishing messages to queues
│   │   │       │
│   │   │       └── function
│   │   │           ├── Function.java             # Core utility functions
│   │   │           └── QueueMappingRequest.java  # Handles queue request mappings
│   │   │
│   │   └── resources
│   │       ├── application.properties            # Configuration properties
│   │       └── logback.xml                       # Logging configuration
│   │
│   └── test
│       └── java
│           └── com
│               ├── domain
│               │   ├── AuthorisationTest.java    # Unit tests for Authorisation
│               │   └── PublishMessageTest.java   # Unit tests for PublishMessage
│               └── function
│                   ├── FunctionTest.java         # Unit tests for Function
│                   └── QueueMappingRequestTest.java  # Unit tests for QueueMappingRequest
│
└── target                      # Directory for compiled classes and build artifacts
```

## Requirements

- **Java 8** or higher
- **Maven** for building the project and managing dependencies

## Setup

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/mahirgamal/PublishMessageMicroservice.git
   ```
2. **Navigate to the Project Directory:**
   ```bash
   cd PublishMessageMicroservice
   ```
3. **Build the Project using Maven:**
   ```bash
   mvn clean install
   ```
4. **Run the Application:**
   ```bash
   java -jar target/PublishMessageMicroservice.jar
   ```

## Usage

1. **Queue Mapping:**
   - Get queue mappings in `QueueMappingRequest.java`.
   - This allows different types of requests to be mapped to the correct queues.

2. **Publishing Messages:**
   - Use the `PublishMessage` class to send messages to the appropriate queue.
   - Example usage:
     ```java
     PublishMessage.publish(Username, Password, queueMappingRequest.getEvent, auth.getId, jsonMessage);
     ```

3. **Authorisation:**
   - Implement and customise authorisation logic in `Authorisation.java`.
   - This ensures that only authorised users can publish messages.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any improvements or bug fixes.

## Acknowledgments

This work originates from the Trakka project and builds on the existing TerraCipher Trakka implementation. We appreciate the support and resources provided by the Trakka project team. Special thanks to Dave Swain and Will Swain from TerraCipher for their guidance and assistance throughout this project.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](https://github.com/mahirgamal/PublishMessageMicroservice/blob/main/LICENSE) file for details.

## Contact

If you have any questions, suggestions, or need assistance, please don't hesitate to contact us at [mhabib@csu.edu.au](mailto:mhabib@csu.edu.au) or [akabir@csu.edu.au](mailto:akabir@csu.edu.au).
