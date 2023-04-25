/**
 *  Copyright (C) 2009-2010 by Mar'yan Rachynsky
 *  mrach@users.sourceforge.net
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.linuxorg.pcal.engine;

/**
 * This class is a container for the integers which can be associated with the dates records
 * @author Mar'yan Rachynskyy
 *
 */
public class DateIntsContainer {
	private static final int intsCount = 4;
	private Integer integers[] = new Integer[intsCount];

	/**
	 * Creates a container with all Integers set to null
	 */
	public DateIntsContainer () {
		for(int i = 0; i< intsCount; integers[i++] = null);
	}

	/**
	 * Creates a container with all Integers set to null except the specified one.
	 * If the index is out of bounds - the empty container is silently created.
	 * The integer value is copied to the container.
	 * @param index - the integer index to be set to the specified value
	 * @param integer - the Integer to be stored in the container
	 */
	public DateIntsContainer (int index, Integer integer) {
		for(int i = 0; i< intsCount; i++) { 
			integers[i] = ((i == index)? new Integer(integer): null);
		}
	}

	/**
	 * Gets the stored integer value. If the index is out of bounds, the null is silently returned.
	 * @param index - index of the value to be returned
	 * @return - returns the corresponding integer value or null if not found
	 */
	public Integer getIntValue(int index) {
		if((index>=0) && (index<intsCount)) {
			return integers[index];
		} else {
			return null;
		}
	}

	/**
	 * Stores the integer value in the location specified.
	 * If the index is out of bounds - the function is silently returns.
	 * The integer value is copied to the container.
	 * @param index - the target integer index
	 * @param value - the value to be copied to the container
	 */
	public void setIntValue(int index, Integer value) {
		if((index>=0) && (index<intsCount)) {
			if(value == null) {
				integers[index] = null;
			} else {
				integers[index] = new Integer(value);
			}
		}		
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String result = ""; //$NON-NLS-1$
		for(int i = 0; i< intsCount; i++) {
			result += (integers[i]==null)?",":integers[i].toString()+","; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result.substring(0, result.length()-1);

	}

	/**
	 * Parse the comma-separated integers list from the string.
	 * @param source - the source string 
	 * @throws NumberFormatException if any of the CSV element is incorrect
	 */
	public static DateIntsContainer parseFromStringFactory(String source) throws NumberFormatException {
		DateIntsContainer result = new DateIntsContainer();
		String listItems[] = source.split(","); //$NON-NLS-1$
		int itemsCount = listItems.length;

		for(int i = 0; i < intsCount; i++) {
			if(i<itemsCount) {
				if(!listItems[i].equals("")) { //$NON-NLS-1$
					result.setIntValue(i, Integer.parseInt(listItems[i]));
				}
			}
		}
		return result;
	}

	/**	
	 * @return the hard-coded container size
	 */
	public static int getIntsCount() {
		return intsCount;
	}
	
}
