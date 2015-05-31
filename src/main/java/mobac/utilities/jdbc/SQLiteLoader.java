/*******************************************************************************
 * Copyright (c) MOBAC developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.utilities.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.swing.JOptionPane;

import mobac.utilities.I18nUtils;

import org.apache.log4j.Logger;

/**
 * Dynamic loading of SqliteJDBC http://www.zentus.com/sqlitejdbc/
 */
public class SQLiteLoader {

	private static final Logger log = Logger.getLogger(SQLiteLoader.class);

	private static boolean SQLITE_LOADED = false;

	private static final String SQLITE_DRIVERNAME1 = "SQLite.JDBCDriver";
	private static final String SQLITE_DRIVERNAME2 = "org.sqlite.JDBC";

	public static boolean loadSQLiteOrShowError() {
		try {
			SQLiteLoader.loadSQLite();
			return true;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, SQLiteLoader.getMsgSqliteMissing(),
					I18nUtils.localizedStringForKey("msg_environment_slqite_lib_missing_title"),
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	public static String getMsgSqliteMissing() {
		return I18nUtils.localizedStringForKey("msg_environment_slqite_lib_missing");
	}

	public static synchronized void loadSQLite() throws SQLException {
		try {
			SQLiteLoader.loadSQLite(SQLITE_DRIVERNAME1);
		} catch (Exception e) {
		}
		SQLiteLoader.loadSQLite(SQLITE_DRIVERNAME2);
	}

	protected static synchronized void loadSQLite(String driverClassName) throws SQLException {
		if (SQLITE_LOADED)
			return;
		try {
			// Load the sqlite library
			Class.forName(driverClassName);
			SQLITE_LOADED = true;
			log.debug("SQLite library loaded. Driver class name: " + driverClassName);
			return;
		} catch (Throwable t) {
			SQLException e = new SQLException("Loading of SQLite library failed (" + driverClassName + "): "
					+ t.getMessage(), t);
			log.error(e.getMessage());
			throw e;
		}
	}

	public static void closeConnection(Connection conn) {
		if (conn == null)
			return;
		try {
			conn.close();
		} catch (Exception e) {
			log.error("Failed to close SQL connection: " + e.getMessage());
		}
	}

}
