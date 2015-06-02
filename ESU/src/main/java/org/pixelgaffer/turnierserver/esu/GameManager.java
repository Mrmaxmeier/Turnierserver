package org.pixelgaffer.turnierserver.esu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.pixelgaffer.turnierserver.esu.utilities.*;

import javafx.collections.*;

public class GameManager {

	public ObservableList<Game> games = FXCollections.observableArrayList();
	
	
	/**
	 * Lädt alle Spieler aus dem Dateisystem in die Liste
	 */
	public void loadGames(){
		games.clear();
		File dir = new File(Paths.gameFolder());
		dir.mkdirs();
		File[] dirs = dir.listFiles();
		if (dirs == null){
			ErrorLog.write("keine Spieler vorhanden");
			return;
		}
		for (int i = 0; i < dirs.length; i++){
			games.add(new Game(dirs[i].getName()));
		}
		
	}
}
