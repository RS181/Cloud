# Cloud
Project for Cloud System Administration 


# how to run 

+ git clone git@github.com:RS181/Cloud.git

+ cd ./Cloud/Rest_API_```XXXX```/jax-rs/jersey/jersey-jetty/

    + Substituir ```XXXX``` por a **Replica** ou **Servidor** dependendo qual queremos usar 

+ mvn clean package 

+ java -jar target/jersey-jetty.jar 


# Test in browser 

+ **Replica**
    
    + http://localhost:```8080```/hello/TESTE

+ **Servidor**
    + http://localhost:```4040```/hello/TESTE


# Location of Source files 

        (...)                               
            └── jax-rs
                ├── README.md
                └── jersey
                    ├── jersey-jetty
                    │   ├── README.md
                    │   ├── pom.xml
                    │   ├── src             ---> MAIN SOURCE FILE 
                    │   │   ├── main
                    │   │   │   └── java
                    │   │   │       └── com
                    │   │   │           └── mkyong
                    │   │   │               ├── MainApp.java
                    │   │   │               ├── MyResource.java
                    │   │   │               └── User.java
                    │   │   └── test
                    │   │       └── java
                    │   │           └── com
                    │   │               └── mkyong
                    │   │                   └── MyResourceTest.java
                    │   └── target
