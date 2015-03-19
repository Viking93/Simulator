package cz.agents.dbtokmlexporter.kmlitem.builder;

import com.vividsolutions.jts.geom.Geometry;

import cz.agents.alite.googleearth.updates.Kmz;
import cz.agents.dbtokmlexporter.kmlitem.InterpolatedTimeKmlItem.TimeRecords;
import cz.agents.dbtokmlexporter.utils.TimeUtils;
import cz.agents.resultsvisio.kml.KmlItem;
import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LookAt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * 
 * @author Marek Cuchy
 * 
 */
public abstract class SimpleKmlItemBuilder {

    protected final String schemaName;

	protected final String fileName;

	protected final boolean hasToBeSavedToKmz;

	public SimpleKmlItemBuilder(String schemaName, String fileName) {
		this(schemaName, fileName, false);
	}

	public SimpleKmlItemBuilder(String schemaName, String fileName,
                                boolean hasToBeSavedToKmz) {
		super();
        this.schemaName = schemaName;
		this.fileName = fileName;
		this.hasToBeSavedToKmz = hasToBeSavedToKmz;
	}

	public Geometry getGeometry(ResultSet resultSet, String columnName) throws SQLException{
		return (Geometry) resultSet.getObject(columnName);
	}

	public abstract KmlItem buildKmlItem() throws SQLException;

	/**
	 * Save {@link cz.agents.resultsvisio.kml.KmlItem}s created by {@code builders} to one {@code kmz} file
	 * named {@code fileName}.
	 *
	 * @param builders
	 *            that creates the {@code KmlItem}s to be saved.
	 * @param fileName
	 *            name of file to which the results are saved.
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 */
	public static void saveBuiltKmlItemsToOneKmz(List<? extends SimpleKmlItemBuilder> builders,
                                                 String fileName) throws SQLException,
			IOException {
		Kml mainKml = new Kml();
		Kmz kmz = new Kmz(mainKml);
		Document doc = mainKml.createAndSetDocument();
		doc.setOpen(true);

		for (SimpleKmlItemBuilder builder : builders) {

			Folder folder = builder.buildKmlItem().initFeatureForKml(kmz);
			if (folder == null) {
				return;
			}
			folder.setName(builder.getFileName());
			doc.addToFeature(folder);

		}
		kmz.writeToStream(new FileOutputStream(fileName + ".kmz"));
	}

	/**
	 * Save {@link cz.agents.resultsvisio.kml.KmlItem}s created by {@code builders} each in separate file
	 * to folder named {@code folderName}.
	 *
	 * @param builders
	 *            that creates the {@code KmlItem}s to be saved.
	 * @param folderName
	 *            name of folder to which the files are saved. Has to be without
	 *            ending {@code '/'}
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 */
	public static void saveBuiltKmlItemsToSeparateFiles(List<? extends SimpleKmlItemBuilder> builders, String folderName, String additionalResourcesFolderPath)
			throws SQLException, IOException {
		File folder = new File(folderName);
		folder.mkdir();

		if (!folder.exists()) {
			throw new IllegalArgumentException("Folder " + folderName
					+ " doesn't exist and the attempt to create it failed.");
		}

		for (SimpleKmlItemBuilder builder : builders) {

			String fileName = folderName + "/" + builder.fileName;
			if (builder.hasToBeSavedToKmz) {
				saveToKmz(builder.buildKmlItem(), fileName, additionalResourcesFolderPath);
			} else {
				saveToKml(builder.buildKmlItem(), fileName);
			}

		}
	}

	public static void saveToKmz(KmlItem output, String path, String additionalResourcesFolderPath) throws FileNotFoundException, IOException {

		Kml kml = new Kml();
		Kmz kmz = new Kmz(kml);

		Folder folder = output.initFeatureForKml(kmz);
		
		if (folder == null) {
			return;
		}

		kml.createAndSetDocument().addToFeature(folder);

		// add additional resources (icon image files) to KMZ
		File additionalResFolder = new File(additionalResourcesFolderPath);
		if (additionalResFolder.exists()) {
			for (File f : additionalResFolder.listFiles()) {
				kmz.loadFile(f);
			}
		} else {
			Logger.getLogger(SimpleKmlItemBuilder.class).error("Folder with additional resources ("+additionalResFolder.toString()+") not found! Icons might not work in the visualization.");
		}
		
		kmz.writeToStream(new FileOutputStream(path));
	}

	public static void saveToKml(KmlItem output, String path) throws FileNotFoundException {
		Kml kml = new Kml();
		kml.createAndSetDocument().addToFeature(output.initFeatureForKml(null));
		kml.marshal(new File(path));
	}

	protected static String formatMillisToIntervalString(long millis) {
		return TimeUtils.formatMillisToString(millis, "HH:mm:ss.SSS");
	}

	protected static Coordinate[] convertJTSCoordinatesToKmlCoordinates(
			com.vividsolutions.jts.geom.Coordinate[] coordinates) {
		Coordinate[] coords = new Coordinate[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			coords[i] = new Coordinate(coordinates[i].x, coordinates[i].y);
		}
		return coords;
	}

	public String getFileName() {
		return fileName;
	}

	protected String getTransformedGeomSql() {
		return "st_transform(geom,4326) as " + getTransformedGeomColumnName();
	}

	protected String getTransformedGeomColumnName() {
		return "tranformed_geom";
	}

}
