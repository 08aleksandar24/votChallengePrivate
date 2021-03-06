/* THIS FILE IS A MEMBER OF THE COFFEESHOP LIBRARY
 * 
 * License:
 * 
 * Coffeeshop is a conglomerate of handy general purpose Java classes.  
 * 
 * Copyright (C) 2006-2008 Luka Cehovin
 * This library is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as 
 *  published by the Free Software Foundation; either version 2.1 of 
 *  the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 *  GNU Lesser General Public License for more details. 
 *  
 *  http://www.opensource.org/licenses/lgpl-license.php
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the 
 *  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA 
 * 
 * 
 * This code is based on JSAP project from Martian Software, Inc.
 * http://www.martiansoftware.com/
 */

package org.coffeeshop.string.parsers;

import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import org.coffeeshop.string.StringUtils;

/**
 * A {@link org.coffeeshop.string.parsers.StringParser} that enforces a limited set of String options for its
 * values.
 * These values are provided in the constructor together with one or two parameters
 * that control the processing of these values.
 * 
 * <p>EnumeratedStringParser was generously contributed to JSAP by Klaus-Peter Berg of Siemens AG, Munich, Germany.
 * @since 1.03
 * @author  Klaus-Peter Berg, Siemens AG, Munich, Germany
 * @version 2.0
 */
public class EnumeratedStringParser implements StringParser {

	/**
	 * char used to separate enumerated values when they are supplied 
	 * to the constructor
	 */
	public static final char CONSTRUCTOR_VALUE_SEPARATOR = ';';
	

	private Object[] validOptionValuesArray = null;

	private String[] validOptionStringsArray = null;
	
	private boolean isCaseSensitive;
	
	private boolean checkOptionChars;

	/**
	 * Constructs a new instance of EnumeratedParameterParser.
	 * 
	 * @param validOptionValues a string that contains valid values for an option 
	 *        in the format "value_1;value_2;..;value_n"; spaces between values are allowed 
	 *        to make things more readable, e.g., "value_1; value_2";
	 *        option values have to be constructed using Java identifier characters
	 *        if the checkOptionChars parameter tells the parser to do this.
	 * @param caseSensitive tells the parser whether the option value is case sensitive
	 * @param checkOptionChars tells the parser whether to check for Java identifier conformant characters.
	 * @throws IllegalArgumentException if the option value string has wrong format
	 *         or is empty
	 */
	public EnumeratedStringParser(String validOptionValues, boolean caseSensitive, boolean checkOptionChars) throws IllegalArgumentException {
		if (validOptionValues == null) {
			throw new IllegalArgumentException("EnumeratedStringParser validOptions parameter is null");
		}
		if (validOptionValues.length() == 0) {
			throw new IllegalArgumentException("EnumeratedStringParser validOptions parameter is empty");
		}

		this.isCaseSensitive = caseSensitive;
		this.checkOptionChars = checkOptionChars;
		if (validOptionValues.indexOf(CONSTRUCTOR_VALUE_SEPARATOR) == -1) {
			validOptionValuesArray = new String[1];	// we assume to have only one valid option value
			if (isValidOptionName(validOptionValues)) {
				validOptionValuesArray[0] = validOptionValues;
			}
			else {
				throw new IllegalArgumentException("Wrong character in EnumeratedStringParser option value: "+validOptionValues
					+ "\nsee EnumeratedStringParser javadoc for more information");
			}
		}
		else {
			StringTokenizer stok = new StringTokenizer(validOptionValues, ";");
			validOptionValuesArray = new String[stok.countTokens()];
			int i = 0;
			while (stok.hasMoreTokens()) {
				String value = stok.nextToken().trim();
				if (!isCaseSensitive) {
					value = value.toLowerCase();
				}
				if (isValidOptionName(value)) {
					validOptionValuesArray[i++] = value;
				}
				else {
					throw new IllegalArgumentException("Wrong character in EnumeratedStringParser option value: "+value
						+ "\nsee EnumeratedStringParser javadoc for more information");
				}               
			}
		}

		validOptionStringsArray = StringUtils.toStrings(validOptionValuesArray);
		
	}

	/**
	 * Constructs a new instance of EnumeratedStringParser.
	 * 
	 */
	public EnumeratedStringParser(Class<?> validOptionValues, boolean caseSensitive) throws IllegalArgumentException {
		if (validOptionValues == null || !validOptionValues.isEnum()) {
			throw new IllegalArgumentException("EnumeratedStringParser validOptions parameter is null");
		}
		Enum<?>[] e = (Enum<?>[]) validOptionValues.getEnumConstants();
		
		if (e == null || e.length == 0) {
			throw new IllegalArgumentException("EnumeratedStringParser validOptions parameter is empty");
		}

		this.isCaseSensitive = caseSensitive;

		validOptionValuesArray = new String[e.length];
		
		for (int i = 0; i < e.length; i++) {
			validOptionValuesArray[i] = e[i].name();
		}
		
		validOptionStringsArray = StringUtils.toStrings(validOptionValuesArray);
	}

	/**
	 * Constructs a new instance of EnumeratedStringParser.
	 * 
	 */
	public EnumeratedStringParser(Iterable<?> values, boolean caseSensitive) throws IllegalArgumentException {

		if (values == null) {
			throw new IllegalArgumentException("EnumeratedStringParser validOptions parameter is empty");
		}

		this.isCaseSensitive = caseSensitive;

		TreeSet<String> validOptions = new TreeSet<String>();
		Vector<Object> options = new Vector<Object>();
		
		for (Object v : values) {
			if (v == null) continue;
			if (validOptions.add(v.toString()))
				options.add(v);
		}
		
		validOptionValuesArray = options.toArray(new Object[validOptions.size()]); 
		
		validOptionStringsArray = StringUtils.toStrings(validOptionValuesArray);
	}
	
	/**
	 * Parses the specified argument, making sure it matches one of the valid
	 * options supplied to its constructor.  
	 * If the specified argument is not a valid option, 
	 * a ParseException is thrown.
	 * 
	 * @param arg the argument to parse
	 * @return the String resulting from the parsed argument.
	 * @throws ParseException if the specified argument cannot be parsed.
	 */
	public Object parse(String arg) throws ParseException {
		if (arg == null) {
			return null;
		}
		if (!isCaseSensitive) {
			arg = arg.toLowerCase();
		}
		if (!isValidOptionName(arg)) {
			throw new ParseException("Wrong character in command line option value for enumerated option: '" + arg + "'"
				+"\nallowed are alphanumeric characters + '$' and '_' sign only",
				new IllegalArgumentException());
		}
		// we cannot use Arrays.binarySearch() because strings cannot be 
		// sorted according to the required natural order!
		for (int i = 0; i < validOptionStringsArray.length; i++) {
			if (validOptionStringsArray[i].equals(arg))
				return validOptionValuesArray[i];
		}
		
		throw new ParseException("Option has wrong value '" + arg + "'"
				+ "; valid values are: "+Arrays.asList(validOptionValuesArray), new IllegalArgumentException());
		
	}

	/**
	 * Check for valid enumerated option values ("names").
	 * Allowed are Java identifier chars, i.e., alphanumeric chars + '$' + _' signs.
	 * If you need a different validation scheme you can override this method
	 * when subclassig EnumeratedStringParser.
	 * 
	 * @param name   the option value to check
	 * 
	 * @return true, if the value contains only valid chars, false otherwise
	 */
	protected boolean isValidOptionName(String name) {
		if (!checkOptionChars) {
			return true;
		}
		for (int i=0; i<name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isJavaIdentifierPart(c)) {
				continue;
			}
			else {
				return false;
			}
		}
		return true;
	}
	
	public Object[] getValues() {
		
		return Arrays.copyOf(validOptionValuesArray, validOptionValuesArray.length);
		
	}
	
	public String[] getTextValues() {
		
		return Arrays.copyOf(validOptionStringsArray, validOptionStringsArray.length);
				
	}
	
	public int findValue(String name) {
		
		if (name == null) return -1;
		
		for (int i = 0; i < validOptionStringsArray.length; i++) {
			if (validOptionStringsArray[i].equals(name))
				return i;
		}
		
		return -1;
	}
	
}
