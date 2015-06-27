package org.pixelgaffer.turnierserver.gamelogic.interfaces;

import java.io.IOException;

import org.pixelgaffer.turnierserver.gamelogic.GameLogic;

import lombok.Setter;

public class AiObject {
	
	/**
	 * True, wenn die AI schon verloren hat (z.B. wenn sie aufgegeben hat)
	 */
	public boolean lost = false;
	
	/**
	 * Die übrigen Rechenpunkte dieser AI
	 */
	public int millisLeft;
	
	/**
	 * Der score der AI, der am Ende in die Datenbank gespeichert wird (z.B. +1 für den Sieger, -1 für den Verlierer)
	 */
	public int score = 0;
	
	@Setter
	private GameLogic<?, ?> logic;
	@Setter
	private Ai ai;
	
	/**
	 * Gibt an, ob geupdated werden soll
	 */
	private boolean updating = false;
	
	/**
	 * Das letzte mal als startCalculationTimer oder updateCalculationTimer aufgerufen wurde
	 */
	private long lastCalculationStart = -1;
	
	/**
	 * Startet den Berechnungtimer
	 */
	public void startCalculationTimer() {
		startCalculationTimer(-1);
	}
	
	/**
	 * Startet den Berechnungtimer
	 * 
	 * @param Die Intervalle, in welchen die Rechenpunkte geupdated werden sollen (<= 0, wenn sie nicht automatisch geupdated werden sollen)
	 */ 
	public void startCalculationTimer(final int updateTime) {
		if(lastCalculationStart != -1) {
			return;
		}
		System.out.println("Calculation timer started! " + millisLeft);
		lastCalculationStart = System.currentTimeMillis();
		if(updateTime > 0) {
			updating = true;
			new Thread(() -> {
				while(updating) {
					try {
						Thread.sleep(updateTime);
					} catch (Exception e) {
						return;
					}
					updateCalculationTimer();
				}
			}).start();
		}
	}
	
	/**
	 * Hält den Berechnungstimer an
	 */
	public boolean stopCalculationTimer() {
		if(lastCalculationStart == -1) {
			return lost;
		}
		System.out.println("Calculation timer stopped!");
		if(updating) {
			updating = false;
		}
		millisLeft -= System.currentTimeMillis()  - lastCalculationStart;
		lastCalculationStart = -1;
		if(millisLeft <= 0) {
			loose();
		}
		return lost;
	}
	
	/**
	 * Updated die übrige Berechnungszeit
	 */
	public void updateCalculationTimer() {
		if(lastCalculationStart == -1) {
			return;
		}
		millisLeft -= System.currentTimeMillis()  - lastCalculationStart;
		System.out.println("Calculation timer updated, millis left: " + millisLeft);
		lastCalculationStart = System.currentTimeMillis();
		if(millisLeft <= 0) {
			System.out.println("Die AI " + ai.getId() + " hat verloren, da sie keine zeit mehr hat");
			loose();
		}
	}
	
	public void loose() {
		if(lost) {
			boolean finishGame = true;
			for(Ai ai : logic.getGame().getAis()) {
				if(!ai.getObject().lost) {
					finishGame = false;
					break;
				}
				System.out.println("AiObject:121: Die KI " + ai.getId() + " hat verloren");
			}
			if(finishGame) {
				logic.endGame();
			}
			return;
		}
		try {
			ai.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		lost = true;
		logic.lost(ai);
		boolean finishGame = true;
		for(Ai ai : logic.getGame().getAis()) {
			if(!ai.getObject().lost) {
				finishGame = false;
				break;
			}
			System.out.println("AiObject:121: Die KI " + ai.getId() + " hat verloren");
		}
		if(finishGame) {
			logic.endGame();
		}
	}
	
}
