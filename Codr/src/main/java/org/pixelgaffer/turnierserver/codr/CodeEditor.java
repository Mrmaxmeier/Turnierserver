package org.pixelgaffer.turnierserver.codr;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;

import javax.xml.parsers.ParserConfigurationException;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.pixelgaffer.katepartparser.EmptyStyle;
import org.pixelgaffer.katepartparser.Style;
import org.pixelgaffer.katepartparser.Styles;
import org.pixelgaffer.katepartparser.SyntaxFileResolver;
import org.pixelgaffer.katepartparser.SyntaxParser;
import org.pixelgaffer.turnierserver.codr.utilities.ErrorLog;
import org.pixelgaffer.turnierserver.codr.utilities.Paths;
import org.xml.sax.SAXException;

/**
 * Managet die Darstellung des Code-Editors.
 * 
 * @author Dominic
 */
public class CodeEditor
{
	public static void writeSyntax () throws ParserConfigurationException, SAXException
	{
		File syntaxFolder = new File(Paths.syntaxFolder());
		if (syntaxFolder.exists())
			return;
		
		try
		{
			syntaxFolder.mkdirs();
			
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(syntaxFolder,
					"_fallback.xml")), UTF_8));
			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			out.println("<!DOCTYPE language SYSTEM \"language.dtd\">");
			out.println("<language name=\"fallback\" section=\"\" extensions=\"*\" mimetype=\"\" priority=\"-100\">");
			out.println("  <highlighting>");
			out.println("    <contexts>");
			out.println("      <context name=\"cdata\" attribute=\"normal\" lineEndContext=\"#stay\">");
			out.println("      </context>");
			out.println("    </contexts>");
			out.println("    <itemDatas>");
			out.println("      <itemData name=\"normal\" defStyleNum=\"dsNormal\" />");
			out.println("    </itemDatas>");
			out.println("  </highlighting>");
			out.println("</language>");
			out.close();
			
			File tmp = File.createTempFile("syntax", ".zip");
			tmp.deleteOnExit();
			FileUtils.copyURLToFile(CodeEditor.class.getResource("syntax.zip"), tmp);
			ZipFile zipFile = new ZipFile(tmp);
			zipFile.extractAll(syntaxFolder.getAbsolutePath());
			
			p = new Properties();
			for (String filename : syntaxFolder.list())
			{
				if (filename.equals("_fallback.xml"))
					continue;
				
				SyntaxParser parser = new SyntaxParser(new File(syntaxFolder, filename), resolver, new EmptyStyle());
				p.put("names." + parser.getName(), filename);
				if (parser.isHidden())
					continue;
				for (String extension : parser.getExtensions())
				{
					extension = extension.trim();
					if (p.containsKey("extensions." + extension))
					{
						if (Integer.parseInt(p.getProperty("extensions." + extension + ".priority")) > parser.getPriority())
							continue;
					}
					p.put("extensions." + extension, filename);
					p.put("extensions." + extension + ".priority", Integer.toString(parser.getPriority()));
				}
			}
			p.store(new FileOutputStream(new File(syntaxFolder, "index.prop")),
					"Enthält alle Syntax-Dateien sortiert nach den Dateienden und den Namen");
		}
		catch (ZipException | IOException e)
		{
			ErrorLog.write("Konnte Syntax nicht schreiben: " + e);
			syntaxFolder.delete();
		}
	}
	
	private static Properties p = null;
	private static final SyntaxFileResolver resolver =
			(name) -> new File(Paths.syntaxFolder(), p.getProperty("names." + name, "_fallback.xml"));
	
	private static Properties props () throws IOException
	{
		if (p == null)
		{
			p = new Properties();
			p.load(new FileInputStream(new File(Paths.syntaxFolder(), "index.prop")));
		}
		return p;
	}
	
	
	private String savedText = "";
	private File document;
	public boolean loaded = false;
	private CodeArea codeArea;
	private SyntaxParser parser;
	
	
	/**
	 * Initialisiert den CodeEditor mit der zu zeigenden Datei
	 * 
	 * @param doc
	 */
	public CodeEditor (File doc)
	{
		document = doc;
		codeArea = new CodeArea();
		codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
		// codeArea.setStyle("-fx-text-fill: white");
		
		Style style = new EmptyStyle();
		try
		{
			style = MainApp.cStart.btTheme.isSelected() ? Styles.getStyle("VibrantInk") : Styles.getStyle("Eclipse");
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		try
		{
			for (Object o : props().keySet())
			{
				String extension = (String)o;
				if (!extension.startsWith("extensions.") || extension.endsWith(".priority"))
					continue;
				extension = extension.substring("extensions.".length());
				extension = extension.replace(".", "\\.");
				extension = extension.replace("*", ".*");
				Pattern pattern = Pattern.compile(extension, Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(doc.getName());
				if (matcher.matches())
				{
					parser = new SyntaxParser(new File(Paths.syntaxFolder(), (String)props().get(o)), resolver, style);
					System.out.println("Loading " + parser.getName() + " for file " + doc);
					break;
				}
			}
		}
		catch (SAXException | ParserConfigurationException | IOException e)
		{
			ErrorLog.write("Fehler beim Laden des SyntaxParser: " + e);
			e.printStackTrace();
		}
		
		if (parser == null)
		{
			try
			{
				parser = new SyntaxParser(new File(Paths.syntaxFolder(), "_fallback.xml"), resolver, style);
			}
			catch (ParserConfigurationException | IOException | SAXException e)
			{
				ErrorLog.write("Fehler beim Laden des SyntaxParser: " + e);
				e.printStackTrace();
			}
		}
		
		if (parser != null)
		{
			codeArea.setId("codeArea");
			try
			{
				codeArea.getStylesheets().add(parser.generateStylesheet("codeArea").toExternalForm());
			}
			catch (IOException e)
			{
				ErrorLog.write("Fehler beim Laden eines generierten StyleSheets: " + e);
				e.printStackTrace();
			}
			codeArea.textProperty().addListener(
					(obs, oldText, newText) -> codeArea.setStyleSpans(0, parser.computeHighlighting(newText)));
		}
		
		load();
	}
	
	
	/**
	 * Überprüft, ob der angezeigte Text mit seiner gespeicherten Datei
	 * übereinstimmt
	 * 
	 * @return true, wenn savedText != text
	 */
	public boolean hasChanged ()
	{
		if (loaded)
			return !savedText.equals(getCode());
		else
			return false;
	}
	
	
	public String getCode ()
	{
		return codeArea.getText();
	}
	
	
	public void setCode (String t)
	{
		codeArea.replaceText(0, codeArea.getText().length() - 1, t);
	}
	
	
	/**
	 * Erstellt ein Tab, das in die Tab-Leiste eingefügt werden kann
	 * 
	 * @return das erstellte Tab
	 */
	public Tab getView ()
	{
		BorderPane pane = new BorderPane(codeArea);
		return new Tab(document.getName(), pane);
	}
	
	
	/**
	 * Lädt den Inhalt der Datei in die StringProperty text
	 */
	public void load ()
	{
		try
		{
			setCode(FileUtils.readFileToString(document));
			
			savedText = getCode();
			loaded = true;
		}
		catch (IOException e)
		{
			ErrorLog.write("Quellcode konnte nicht gelesen werden: " + e);
		}
	}
	
	
	/**
	 * Speichert den Inhalt der StringProperty text in die Datei
	 * 
	 * @return false wenn nichts geändert wurde, ansonsten true
	 */
	public boolean save ()
	{
		if (!hasChanged())
			return false;
		forceSave();
		return true;
	}
	
	
	/**
	 * Speichert den Inhalt der StringProperty text in eine Datei. Dabei wird
	 * nicht Überprüft, ob sich der Inhalt verändert hat.
	 */
	public void forceSave ()
	{
		try
		{
			FileWriter writer = new FileWriter(document, false);
			writer.write(getCode());
			writer.close();
			savedText = getCode();
		}
		catch (IOException e)
		{
			ErrorLog.write("Quellcode konnte nicht bearbeitet werden");
		}
	}
	
}
