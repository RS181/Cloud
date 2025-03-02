package com.mkyong;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

//class that contains all related to the Database
public class DB {
    // Connect to MySql
    public static Connection connection;
    

    //Open acess to database
    public static Connection OpenDatabase() {
        String url = "jdbc:mysql://localhost:3306/clouddb";
        String user= "root";
        String password = "root";
	try {
            Class.forName("com.mysql.jdbc.Driver");

            connection = DriverManager.getConnection(url,user,password);
            System.out.println("=====> Connection is Sucessful to the database <=====");

            return connection;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    //Close acess to database
    public static void CloseDatabase(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}