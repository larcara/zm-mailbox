/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jan 31, 2005
 */
package com.zimbra.cs.mime;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.zimbra.cs.mailbox.Contact;


public class ParsedAddress {

    private static String  HONORIFIC = "([^,\\.\\s]{2,}\\.\\s+)?";
    private static String  INITIAL = "(?:[^,\\s]\\.\\s*)";
    private static String  FIRST_NAME = "(" + INITIAL + "{2,}|" + INITIAL + "?[^,\\s]+)";
    private static String  MIDDLE_NAME = "[^,(;{\\[]*";
    private static String  LAST_NAME = "(\\S+)";
    private static Pattern NAME_SPACE_PATTERN = Pattern.compile(HONORIFIC + FIRST_NAME + "\\s+(.*)");
    private static Pattern COMMA_NAME_PATTERN = Pattern.compile(LAST_NAME + ",\\s*(" + HONORIFIC + FIRST_NAME + MIDDLE_NAME + ")(.*)");

	public String emailPart;
	public String personalPart;
    public String honorific;
    public String firstName;
    public String lastName;
    public String suffix;

    public    boolean first  = true;
    protected boolean parsed = false;
    
	public ParsedAddress(String address) {
		try {
            InternetAddress ia = new InternetAddress(address); 
			initialize(ia.getAddress(), ia.getPersonal());
		} catch (AddressException ae) {
			personalPart = address;
		}
	}
    public ParsedAddress(InternetAddress ia) {
        initialize(ia.getAddress(), ia.getPersonal());
    }
    public ParsedAddress(String email, String personal) {
        initialize(email, personal);
    }
    public ParsedAddress(ParsedAddress node) {
        emailPart    = node.emailPart;
        personalPart = node.personalPart;
        honorific    = node.honorific;
        firstName    = node.firstName;
        lastName     = node.lastName;
        suffix       = node.suffix;
        parsed = node.parsed;
    }

    private void initialize(String email, String personal) {
        emailPart    = email;
        personalPart = personal;
        if ("".equals(emailPart))
            emailPart = null;
    }

    public String getSortString() {
        parse();
        return (personalPart != null ? personalPart : emailPart);
    }

    public Map getAttributes() {
        parse();
        HashMap map = new HashMap();
        if (honorific != null)     map.put(Contact.A_namePrefix, honorific);
        if (firstName != null)     map.put(Contact.A_firstName, firstName);
        if (lastName != null)      map.put(Contact.A_lastName, lastName);
        if (personalPart != null)  map.put(Contact.A_fullName, personalPart);
        if (emailPart != null)     map.put(Contact.A_email, emailPart);
        return map;
    }

    public ParsedAddress parse() {
        if (parsed)
            return this;

        if (emailPart != null && emailPart.equals(personalPart))
            personalPart = null;
        if (personalPart != null) {
            if (personalPart.indexOf(' ') == -1 && personalPart.indexOf(',') == -1)
                firstName = personalPart;
            else {
                Matcher m = NAME_SPACE_PATTERN.matcher(personalPart);
                if (m.matches()) {
                    honorific = m.group(1);
                    firstName = m.group(2).trim();
                    lastName = m.group(3).trim();
                } else {
                    m = COMMA_NAME_PATTERN.matcher(personalPart);
                    if (m.matches()) {
                        honorific = m.group(3);
                        firstName = m.group(4);
                        lastName = m.group(1);
                        personalPart = m.group(2).trim() + ' ' + m.group(1);
                        String remainder = m.group(5);
                        if (remainder != null && !remainder.equals("")) {
                            if (!remainder.startsWith(",") && !remainder.startsWith(";"))
                                personalPart += ' ';
                            personalPart += remainder;
                        }
                    }
                }
            }
        }
        if (emailPart != null && firstName == null) {
            int p = emailPart.indexOf('@');
            if (p != -1) {
                String formatted = emailPart.substring(0, p).replace('.', ' ').replace('_', ' ');
                int space = formatted.indexOf(' ');
                firstName = space == -1 ? formatted : formatted.substring(0, space);
                if (space != -1 && personalPart == null)
                    personalPart = formatted;
            }
        }
        parsed = true;
        return this;
    }
    
    public String toString() {
        if (emailPart == null)
            return personalPart;
        else if (personalPart == null)
            return emailPart;
        else
        	return '"' + personalPart + "\" <" + emailPart + '>';
    }
}