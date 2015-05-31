package mobac.utilities.file;

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
import java.io.File;

/**
 * @author Maksym "elmuSSo" Kondej
 * 
 * This filter will pass-through every directory,
 * but all files will be filtered by a FileExtFilter.
 */
public class DirOrFileExtFilter extends FileExtFilter {

	public DirOrFileExtFilter(String acceptedFileExt) {
		super(acceptedFileExt);
	}

	@Override
	public boolean accept(File pathname) {
		if(pathname.isDirectory()) {
			// All directories are accepted
			return true;
		} else {
			// Files are passed to the accept method of FileExtFilter
			return super.accept(pathname);
		}
	}
}