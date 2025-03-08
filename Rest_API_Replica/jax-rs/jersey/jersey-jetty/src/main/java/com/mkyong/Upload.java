package com.mkyong;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.File;

import java.net.URL;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * Class that takes care of the upload of files (given by client's)
 */
@Path("/upload")
public class Upload {

    // Location where we store the Uploadede Resource's
    private static final String STORAGE_FOLDER =  AbsolutePath.STORAGE_FOLDER;

    // 
    private static final String SERVER_UPLOAD_API_URL = AbsolutePath.SERVER_UPLOAD_API_URL;    
    
    private static final String SERVER_TEST_API_URL = AbsolutePath.SERVER_TEST_API_URL;

    /**
     * Test if Upload service is available 
     * @return
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Test connection of /upload";
    }



    /**
     * Upload's a file and save's it in UPLOAD_FOLDER (also upload's it to Main Server)
     * 
     * Note: Since the main server has all the information, there will be no case when a
     * Replica has a file and the main server does not (since each time we upload a movie
     * in a Replica it send's it to the main server)
     * @param uploadedInputStream
     * @param fileDetail
     * @return
     */

    @POST
    @Path("/resource")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadResource(
        @FormDataParam("file") InputStream uploadedInputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetail)
    {
        if (uploadedInputStream == null || fileDetail == null)
            return Response.status(Response.Status.BAD_REQUEST).entity("ERROR: Mising file parameter").build();
        
        String fileName = fileDetail.getFileName();

        File file = new File(STORAGE_FOLDER + fileName);
        

        if (file.exists()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("ERROR: File '" + fileName + "' already exists in storage.")
                    .build();
        }

        try {
            saveToFile(uploadedInputStream, file.getAbsolutePath() );

            if (mainServerOn())
                sendToMainServer(file);

        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("ERROR: error while trying to save File in storage").build();
        }



        return Response.status(Response.Status.OK).entity("Upload Replica feito com sucesso").build();
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


    /**
     * Send's a file to the main server (using Server's API)
     * @param file that is going to be sent to Main Server 
     */
    private void sendToMainServer(File file){
        String command = 
        "curl -X POST " + SERVER_UPLOAD_API_URL  + " -H \"Content-Type: multipart/form-data\"   -F \"file=@" + file.getAbsolutePath() + "\"";

        //System.out.println(command);

        try{
            ProcessBuilder processBuilder = new ProcessBuilder("bash","-c",command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read's the output of process
            //try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            //    String line;
            //    while ((line = reader.readLine()) != null) {
            //        System.out.println(line);
            //    }
            //}


            // Wait for process to end 
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Upload para Servidor principal realizado com sucesso!");
            } else {
                System.out.println("Falha no upload para servidor principal. Código de saída: " + exitCode);
            }
            
        }catch (Exception e){
            System.out.println("SEVERE: PROBLEMA NO UPLOAD DE Replica -> Servidor_Principal");
        }

    }

    /**
     * Saves file to uploadedFileLocation
     * @param uploadedInputStream
     * @param uploadedFileLocation
     * @throws IOException
     */
    private void saveToFile(InputStream uploadedInputStream, String uploadedFileLocation) throws IOException {
        try (OutputStream out = new FileOutputStream(new File(uploadedFileLocation))) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) 
                out.write(bytes, 0, read);

        }
    }

}