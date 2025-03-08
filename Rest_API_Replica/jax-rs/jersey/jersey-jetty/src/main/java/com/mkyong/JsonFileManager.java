package com.mkyong;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Class that add's Metadata of a file (name_of_file.extension) to a specific
 * path
 */
public class JsonFileManager {

    private static final String METADATA_FILE = AbsolutePath.METADATA_FILE;

    /**
     * Add's file metadata to Json file
     * 
     * @param fileName name_of_file.extension
     */
    public static void addMetadataToJson(String fileName) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Ativa indentação

        File jsonFile = new File(METADATA_FILE);

        try {
            JsonNode root;

            // Check if file exists, if so read's it's content;s
            if (jsonFile.exists() && jsonFile.length() > 0) {
                root = objectMapper.readTree(jsonFile);
            } else {
                // If file does not exist, or is empty, create new root
                root = objectMapper.createObjectNode();
            }

            // Check if key "arquivos" exists, if not creates a new array
            ArrayNode arquivosArray;
            if (root.has("arquivos")) {
                arquivosArray = (ArrayNode) root.get("arquivos");
            } else {
                arquivosArray = objectMapper.createArrayNode();
                ((ObjectNode) root).set("arquivos", arquivosArray);
            }

            // Avoid duplicates
            Set<String> existingNames = new HashSet<>();
            for (JsonNode node : arquivosArray)
                existingNames.add(node.get("nome").asText());

            // Only add's if filName doesnt exist already
            if (!existingNames.contains(fileName)) {
                ObjectNode newArquivo = objectMapper.createObjectNode();
                newArquivo.put("nome", fileName);
                arquivosArray.add(newArquivo);

                writePrettyJson(objectMapper, jsonFile, root);

                System.out.println("Arquivo adicionado com sucesso!");
            } else
                System.out.println("O arquivo '" + fileName + "' já existe no JSON.");

        } catch (IOException e) {
            System.err.println("Erro ao manipular o arquivo JSON: " + e.getMessage());
        }
    }

    /**
     * Removes a file metadata from Json file (in case it exists)
     * @param fileName
     */

    public static void removeMetadataFromJson(String fileName){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        File jsonFile = new File(METADATA_FILE);

        try {
            if (!jsonFile.exists() || jsonFile.length() == 0) {
                System.out.println("O arquivo JSON não existe ou está vazio.");
                return;
            }

            JsonNode root = objectMapper.readTree(jsonFile);
            if (!root.has("arquivos")) {
                System.out.println("O JSON não contém a chave 'arquivos'.");
                return;
            }

            ArrayNode arquivosArray = (ArrayNode) root.get("arquivos");
            Iterator<JsonNode> iterator = arquivosArray.elements();
            boolean foundMatch = false;

            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                if (node.get("nome").asText().equals(fileName)) {
                    iterator.remove();
                    foundMatch = true;
                    break;
                }
            }

            if (foundMatch) {
                writePrettyJson(objectMapper, jsonFile,root);
                System.out.println("Arquivo removido com sucesso!");
            } else 
                System.out.println("O arquivo '" + fileName + "' não foi encontrado no JSON.");
        } catch (IOException e) {
            System.err.println("Erro ao manipular o arquivo JSON: " + e.getMessage());
        }

    }


    /**
     * Checks if json has a files Metadata 
     * @param fileName
     * @return true if file exists otherwise false
     */
    public static boolean checkIfMetadatExists(String fileName){
        ObjectMapper objectMapper = new ObjectMapper();
        File jsonFile = new File(METADATA_FILE);

        try {
            if (!jsonFile.exists() || jsonFile.length() == 0) {
                return false; // JSON file is empty or does not exist
            }

            JsonNode root = objectMapper.readTree(jsonFile);
            if (!root.has("arquivos")) {
                return false; // JSON does not contain key "arquivos"
            }

            ArrayNode arquivosArray = (ArrayNode) root.get("arquivos");
            for (JsonNode node : arquivosArray) {
                if (node.get("nome").asText().equals(fileName)) {
                    return true; // Found Match
                }
            }

        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo JSON: " + e.getMessage());
        }
        return false; // Não encontrou o arquivo
    }



    private static void writePrettyJson(ObjectMapper objectMapper, File jsonFile, JsonNode root)
            throws JsonProcessingException, FileNotFoundException, UnsupportedEncodingException {
        // Custom Preety printer
        PrettyPrinter prettyPrinter = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("    ", "\n"))
                .withArrayIndenter(new DefaultIndenter("    ", "\n"));

        // Write back to JSON with the custom formatting
        try (PrintWriter writer = new PrintWriter(jsonFile, "UTF-8")) {
            writer.write(objectMapper.writer(prettyPrinter).writeValueAsString(root));
        }
    }

    public static void main(String[] args) {
        String nomeArquivo = "documento.pdf";
        String nomeArquivo2= "teste.txt";
        // removeMetadataFromJson(nomeArquivo);
        // removeMetadataFromJson(nomeArquivo2);
        // removeMetadataFromJson(nomeArquivo2);
        addMetadataToJson(nomeArquivo);
        addMetadataToJson(nomeArquivo2);
        // addMetadataToJson(nomeArquivo2);
        System.out.println(checkIfMetadatExists(nomeArquivo2));        

    }

}
