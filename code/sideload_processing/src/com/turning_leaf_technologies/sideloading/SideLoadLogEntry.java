package com.turning_leaf_technologies.sideloading;

import com.turning_leaf_technologies.logging.BaseIndexingLogEntry;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class SideLoadLogEntry implements BaseIndexingLogEntry {
	private Long logEntryId = null;
	private final Date startTime;
	private Date endTime;
	private final ArrayList<String> notes = new ArrayList<>();
	private int numSideLoadsUpdated = 0;
	private String sideLoadsUpdated = "";
	private int numProducts = 0;
	private int numErrors = 0;
	private int numAdded = 0;
	private int numDeleted = 0;
	private int numUpdated = 0;
	private int numSkipped = 0;
	private int numInvalidRecords = 0;
	private final Logger logger;

	SideLoadLogEntry(Connection dbConn, Logger logger) {
		this.logger = logger;
		this.startTime = new Date();
		try {
			insertLogEntry = dbConn.prepareStatement("INSERT into sideload_log (startTime) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS);
			updateLogEntry = dbConn.prepareStatement("UPDATE sideload_log SET lastUpdate = ?, endTime = ?, notes = ?, numSideLoadsUpdated = ?, sideLoadsUpdated = ?, numProducts = ?, numErrors = ?, numAdded = ?, numUpdated = ?, numDeleted = ?, numSkipped = ?, numInvalidRecords = ? WHERE id = ?", PreparedStatement.RETURN_GENERATED_KEYS);
		} catch (SQLException e) {
			logger.error("Error creating prepared statements to update log", e);
		}
		saveResults();
	}

	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	//Synchronized to prevent concurrent modification of the notes ArrayList
	public synchronized void addNote(String note) {
		Date date = new Date();
		this.notes.add(dateFormat.format(date) + " - " + note);
		saveResults();
	}

	private String getNotesHtml() {
		StringBuilder notesText = new StringBuilder("<ol class='cronNotes'>");
		for (String curNote : notes) {
			String cleanedNote = curNote;
			cleanedNote = cleanedNote.replaceAll("<pre>", "<code>");
			cleanedNote = cleanedNote.replaceAll("</pre>", "</code>");
			//Replace multiple line breaks
			cleanedNote = cleanedNote.replaceAll("(?:<br?>\\s*)+", "<br/>");
			cleanedNote = cleanedNote.replaceAll("<meta.*?>", "");
			cleanedNote = cleanedNote.replaceAll("<title>.*?</title>", "");
			notesText.append("<li>").append(cleanedNote).append("</li>");
		}
		notesText.append("</ol>");
		String returnText = notesText.toString();
		if (returnText.length() > 25000) {
			returnText = returnText.substring(0, 25000) + " more data was truncated";
		}
		return returnText;
	}

	private static PreparedStatement insertLogEntry;
	private static PreparedStatement updateLogEntry;

	public boolean saveResults() {
		try {
			if (logEntryId == null) {
				insertLogEntry.setLong(1, startTime.getTime() / 1000);
				insertLogEntry.executeUpdate();
				ResultSet generatedKeys = insertLogEntry.getGeneratedKeys();
				if (generatedKeys.next()) {
					logEntryId = generatedKeys.getLong(1);
				}
			} else {
				int curCol = 0;
				updateLogEntry.setLong(++curCol, new Date().getTime() / 1000);
				if (endTime == null) {
					updateLogEntry.setNull(++curCol, java.sql.Types.INTEGER);
				} else {
					updateLogEntry.setLong(++curCol, endTime.getTime() / 1000);
				}
				updateLogEntry.setString(++curCol, getNotesHtml());
				updateLogEntry.setInt(++curCol, numSideLoadsUpdated);
				updateLogEntry.setString(++curCol, sideLoadsUpdated);
				updateLogEntry.setInt(++curCol, numProducts);
				updateLogEntry.setInt(++curCol, numErrors);
				updateLogEntry.setInt(++curCol, numAdded);
				updateLogEntry.setInt(++curCol, numUpdated);
				updateLogEntry.setInt(++curCol, numDeleted);
				updateLogEntry.setInt(++curCol, numSkipped);
				updateLogEntry.setInt(++curCol, numInvalidRecords);
				updateLogEntry.setLong(++curCol, logEntryId);
				updateLogEntry.executeUpdate();
			}
			return true;
		} catch (SQLException e) {
			logger.error("Error creating updating log", e);
			return false;
		}
	}

	public void setFinished() {
		this.endTime = new Date();
		this.addNote("Finished Processing Side Loads");
		this.saveResults();
	}

	public void incErrors(String note) {
		this.addNote("ERROR: " + note);
		numErrors++;
		this.saveResults();
		logger.error(note);
	}

	public void incErrors(String note, Exception e) {
		this.addNote("ERROR: " + note + " " + e.toString());
		numErrors++;
		this.saveResults();
		logger.error(note, e);
	}

	void incAdded() {
		numAdded++;
	}

	void incDeleted() {
		numDeleted++;
	}

	void incUpdated() {
		numUpdated++;
	}

	@SuppressWarnings("SameParameterValue")
	void incNumProducts(int size) {
		numProducts += size;
	}

	void addUpdatedSideLoad(String sideLoadName) {
		numSideLoadsUpdated++;
		if (sideLoadsUpdated.length() > 0) {
			sideLoadsUpdated += ",";
		}
		sideLoadsUpdated += sideLoadName;
		this.saveResults();
	}

	@SuppressWarnings("unused")
	public boolean hasErrors() {
		return numErrors > 0;
	}

	void incSkipped() {
		numSkipped++;
	}

	int getNumProducts() {
		return numProducts;
	}

	public void incInvalidRecords(String invalidRecordId){
		this.numInvalidRecords++;
		this.addNote("Invalid Record found: " + invalidRecordId);
	}

	public int getNumDeleted() {
		return numDeleted;
	}
}
