package com.mkyong;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Class that handles git stuff
 */
public class GitManager {

    // File where local git repo is located
    public final static String METADATA_DIR = AbsolutePath.METADATA_DIR;


    /**
     * Executes a git pull to https://github.com/RS181/Cloud_Meta_data
     * @return True if command succeded otherwise false
     */
    public static boolean executeGitPull(){
        if (!executeGitCommand("git","pull")){
            System.err.println("Falha ao executar git pull");
            return false;
        }

        System.out.println("Git pull executed was Successful");
        return true;
    }

    /**
     * Executes a git add -> git commit -> git push (in this order)
     * @return True if every command succeded otherwise false
     */
    public static boolean executarGitCommitEPush() {
        if (!executeGitCommand( "git", "add", ".")) {
            System.err.println("Falha ao executar git add.");
            return false;
        }

        if (!executeGitCommand("git", "commit", "-m", "atualização metadados")) {
            System.err.println("Falha ao executar git commit. Talvez não haja mudanças para commitar.");
            return false;
        }

        if (!executeGitCommand( "git", "push")) {
            System.err.println("Falha ao executar git push.");
            return false;
        }

        System.out.println("Git commit e push realizados com sucesso!");
        return true;
    }



    /**
     * Executes a git command
     * @param command push, pull, add or commit
     * @return True if command was sucessful otherwise false
     */
    private static boolean executeGitCommand(String... command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(METADATA_DIR));

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            printCommandOutput(process,command);

            return exitCode == 0; // Returns true if command was Sucessful

        } catch (IOException | InterruptedException e) {
            System.err.println("Erro ao executar comando Git: " + e.getMessage());
            return false;
        }

    }


    private static void printCommandOutput(Process process, String... command) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String line;
            System.out.println("Saída do git " + Arrays.toString(command)+":");
            while ((line = reader.readLine()) != null) {
                System.out.println("   "+line);
            }

            System.out.println("Erros (se houver):");
            while ((line = errorReader.readLine()) != null) {
                System.err.println("   "+line);
            }
        }
    }

    public static void main(String[] args) {

        System.out.println(executeGitPull());
        System.out.println(executarGitCommitEPush());
    }
}