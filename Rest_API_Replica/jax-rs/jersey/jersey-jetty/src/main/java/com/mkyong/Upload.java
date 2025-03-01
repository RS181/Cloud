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
import java.io.File;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
/**
 * Class that takes care of the upload of files 
 */
@Path("/upload")
public class Upload {

    // Location where we store the Uploadede Resource's
    private static final String STORAGE_FOLDER =  "/home/rui/Desktop/Replica_File_Storage/";

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

        try {
            saveToFile(uploadedInputStream, file.getAbsolutePath() );
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("ERROR: error while trying to save File in storage").build();
        }

        return Response.status(Response.Status.OK).entity("Upload feito com sucesso").build();
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