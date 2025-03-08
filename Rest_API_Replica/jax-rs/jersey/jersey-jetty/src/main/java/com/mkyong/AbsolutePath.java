package com.mkyong;

/**
 * Class that contains absolute path used in Replica Api
 */
public class AbsolutePath {
    // Location where we store Uploaded Resource's
    public static final String STORAGE_FOLDER =  "/home/rui/Desktop/Replica_File_Storage/";
    
    // URL to test API of Main Server
    //public static final String SERVER_TEST_API_URL = "http://localhost/replica_api/hello";
    public static final String SERVER_TEST_API_URL = "http://localhost:4040/hello";

    // URL of Upload API of Main server 
    //public static final String SERVER_UPLOAD_API_URL = "http://localhost/server_api/upload-main/resource";    
    public static final String SERVER_UPLOAD_API_URL = "http://localhost:4040/upload-main/resource";    

    // Location where we store Uploaded Resource's Metadata (corresponds to git repo)
    public static final String METADATA_FILE = "/home/rui/Desktop/Cloud_Meta_data/metadata.json";
}
