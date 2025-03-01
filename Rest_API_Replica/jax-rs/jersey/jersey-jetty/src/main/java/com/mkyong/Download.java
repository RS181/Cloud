package com.mkyong;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;


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
    private static final String STORAGE_FOLDER =  "/home/rui/Desktop/Replica_File_Storage/";

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
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("ERROR: File '" + fileName + "' not found in storage.")
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

}
