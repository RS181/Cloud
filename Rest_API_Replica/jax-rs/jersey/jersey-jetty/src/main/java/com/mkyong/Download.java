package com.mkyong;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/*
 * Class that takes care of the download of files (asked by client's)
 */
@Path("/download")
public class Download {
    // Location where we store the Uploadede Resource's
    private static final String STORAGE_FOLDER =  AbsolutePath.STORAGE_FOLDER;
    private static final String SERVER_TEST_API_URL = AbsolutePath.SERVER_TEST_API_URL;

    /**
     * Test if download service is available 
     * @return
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Test connection of /download";
    }

    /**
     * Método para baixar um arquivo existente.
     * @param fileName Nome do arquivo a ser baixado.
     * @return Response com o arquivo se ele existir.
     */
    @GET
    @Path("/resource/{fileName}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("fileName") String fileName) {
        File file = new File(STORAGE_FOLDER + fileName);
        
        if (!file.exists()) {
            // If the file exists in the main server (Donwloaded to this replica)
            if (getFileFromMainServer(fileName,false))
                getFileFromMainServer(fileName, true);
                
            else 
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("ERROR: File '" + fileName + "' not found in storage (local & Server).")
                    .build();
        }
        
        try {
            String mediaType = Files.probeContentType(Paths.get(file.getAbsolutePath()));

            if (mediaType == null)
                mediaType = MediaType.APPLICATION_OCTET_STREAM; // default mediaType

            InputStream fileStream = new FileInputStream(file);

            return Response.ok(fileStream, mediaType)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Length", file.length()) 
                    .build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("ERROR: Unable to process file download.")
                    .build();
        }
    }

    /**
     * Tries to get a file from the main server 
     * 
     * Note: we use startDownload argument to avoid having to separate functions
     * (one for testing if Main server jas file and another to donwload the file)
     * 
     * We only use this method, if this replica does not have a requested 
     * file 
     * @param fileName
     * @param startDownload boolean variable that indicates whether or not to download a file
     * @return True/False depending if Main server has file (startDownload == false) or if 
     * the download was sucessful (starDonwload == true)
     */
    public boolean getFileFromMainServer(String fileName, boolean startDownload){
        if(!mainServerOn())
            return false; 

        String command = "";

        if (!startDownload)
            command = "curl http://localhost:4040/download-main/resource/" + fileName;
        else 
            command = 
                "curl http://localhost:4040/download-main/resource/" + 
                fileName + " > " + STORAGE_FOLDER + fileName  ;
        
        boolean hasFile = true;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("bash","-c",command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Check if Main server has file 
            if (!startDownload){
                // Read's the output of process
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.equals("ERROR: File '"+fileName+"' not found in storage.")){
                            hasFile = false;
                            break;
                        }
                    }
                }
            }
            int exitCode = process.waitFor();

        } catch (Exception e) {
            System.out.println("ERRO: Metodo getFileFromMainServer");
        }
        return hasFile;
    }


    private boolean mainServerOn(){
        
        try{
            URL url = new URL(SERVER_TEST_API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET"); 
            connection.setConnectTimeout(5000); // Time limite for connection (5 sec)
            connection.setReadTimeout(5000);    // Time limit for reading (5 sec)
            
            int responseCode = connection.getResponseCode();

            if (responseCode == 200)
                return true;
            else 
                return false;

        }catch(Exception e){
            System.out.println("MAIN SERVER IS OFFLINE");
        }

        return false;
    }

}
