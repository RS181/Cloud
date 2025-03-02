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
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.File;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
/**
 * Class that takes care of the uploaded files (given by Replicas) to the main server
 */
@Path("/upload-main")
public class Upload {

    // Location where we store the Uploadede Resource's
    private static final String STORAGE_FOLDER =  "/home/rui/Desktop/Servidor_File_Storage/";

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
     * Upload's a file and save's it in UPLOAD_FOLDER
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

        // Atempts to save file in local storage 
        try {
            saveToFile(uploadedInputStream, file.getAbsolutePath() );
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("ERROR: error while trying to save File in storage").build();
        }

        // Attempts to save file in Server's Database
        try{
            String query = "INSERT INTO files (fileid,filelocation,numberofacesses) value(?,?,?)";
            PreparedStatement preparedStmt = MainApp.con.prepareStatement(query);
            preparedStmt.setString(1, fileName);
            preparedStmt.setString(2, file.getAbsolutePath());
            preparedStmt.setInt(3, 0);

            preparedStmt.execute();
            //System.out.println("SUCESS: Added new file to Main Server Database");
        }catch(SQLException e){
            System.out.println("Error ocurred while trying to add a file to the Main Server Database");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("ERROR: error while trying to save File in DATABASE").build();
        }

        return Response.status(Response.Status.OK).entity("Upload (local e para B.D) feito com sucesso").build();
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