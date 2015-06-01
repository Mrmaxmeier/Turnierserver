package utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class ErrorLog {

	private static final File file = new File("errorLog.txt");
	
	/**
	 * l�scht die Datei "errorLog.txt"
	 */
	public static void clear(){
		file.delete();
	}
	
	/**
	 * Speichert einen neuen Eintrag in "errorLog.txt" mit anschlie�ender newLine
	 * 
	 * @param log Inhalt der Fehlermeldung
	 * @param withClock bestimmt, ob die Uhrzeit mit gespeichert werden soll
	 */
	public static void write(String log, boolean withClock){
		if (withClock){
			writeWithoutBlanck(new java.text.SimpleDateFormat("HH:mm,ss").format(new Date()) + ": ");
			write(log);
		}
		else{
			write(log);
		}
	}
	
	/**
	 * �berladene Methode ohne withClock-Auswahl (siehe oben)
	 * 
	 * @param log Inhalt der Fehlermeldung
	 */
	public static void write(String log){
		writeWithoutBlanck(log);
		writeWithoutBlanck(System.getProperty("line.separator"));
	}
	
	/**
	 * �bernimmt den eigentlichen Vorgang des Schreibens
	 * 
	 * @param log Inhalt der Fehlermeldung
	 */
	private static void writeWithoutBlanck(String log){
		try {
			FileWriter writer = new FileWriter(file ,true);
			writer.write(log);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}