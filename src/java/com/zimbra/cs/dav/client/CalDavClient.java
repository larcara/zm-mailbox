/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.dom4j.Element;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.UserServlet.HttpInputStream;

public class CalDavClient extends WebDavClient {

	public static class Appointment {
		public Appointment(String h, String e) {
			href = h; etag = e;
		}
		public Appointment(String h, String e, String d) {
			this(h, e);
			data = d;
		}
		public String href;
		public String etag;
		public String data;
	}
	
	public CalDavClient(String baseUrl) {
		super(baseUrl, "CalDAV client");
	}
	
	public void login(String principalUrl) throws IOException, DavException {
		DavRequest propfind = DavRequest.PROPFIND(principalUrl);
		propfind.addRequestProp(DavElements.E_DISPLAYNAME);
		propfind.addRequestProp(DavElements.E_CALENDAR_HOME_SET);
		Collection<DavObject> response = sendMultiResponseRequest(propfind);
		if (response.size() != 1)
			throw new DavException("invalid response to propfind on principal url", null);
		DavObject resp = response.iterator().next();
		mCalendarHomeSet = new HashSet<String>();
		Element homeSet = resp.getProperty(DavElements.E_CALENDAR_HOME_SET);
		for (Object href : homeSet.elements(DavElements.E_HREF))
			mCalendarHomeSet.add(((Element)href).getText());
		if (mCalendarHomeSet.isEmpty())
			throw new DavException("dav response from principal url does not contain calendar-home-set", null);
	}

	public Collection<String> getCalendarHomeSet() {
		return mCalendarHomeSet;
	}
	
	public Map<String,String> getCalendars() throws IOException, DavException {
		HashMap<String,String> calendars = new HashMap<String,String>();
		for (String calHome : mCalendarHomeSet) {
			for (DavObject obj : listObjects(calHome, null)) {
				String href = obj.getHref();
				String displayName = obj.getDisplayName();
				if (obj.isCalendarFolder() && displayName != null && href != null)
					calendars.put(displayName, href);
			}
		}
		return calendars;
	}

	public Collection<Appointment> getEtags(String calendarUri) throws IOException, DavException {
		ArrayList<Appointment> etags = new ArrayList<Appointment>();
		DavRequest propfind = DavRequest.PROPFIND(calendarUri);
		propfind.setDepth(Depth.one);
		propfind.addRequestProp(DavElements.E_GETETAG);
		propfind.addRequestProp(DavElements.E_RESOURCETYPE);
		Collection<DavObject> response = sendMultiResponseRequest(propfind);
		for (DavObject obj : response) {
			String href = obj.getHref();
			String etag = obj.getPropertyText(DavElements.E_GETETAG);
			if (!obj.isFolder() && etag != null && href != null)
				etags.add(new Appointment(href, etag));
		}
		return etags;
	}
	
	public Appointment getCalendarData(Appointment appt) throws IOException {
		HttpInputStream resp = sendGet(appt.href);
		byte[] res = ByteUtil.getContent(resp, resp.getContentLength());
		appt.data = new String(res, "UTF-8");
		return appt;
	}
	
	public String sendCalendarData(Appointment appt) throws IOException, DavException {
		HttpInputStream resp = sendPut(appt.href, appt.data.getBytes(), Mime.CT_TEXT_CALENDAR, appt.etag);
		String etag = resp.getHeader(DavProtocol.HEADER_ETAG);
		ZimbraLog.dav.debug("ETags: "+appt.etag+", "+etag);
		int status = resp.getStatusCode();
		if (status != HttpStatus.SC_OK && status != HttpStatus.SC_CREATED && status != HttpStatus.SC_NO_CONTENT) {
			throw new DavException("Can't send calendar data (status="+status+")", status);
		}
		return etag;
	}
	
	public Collection<Appointment> getCalendarData(String url, Collection<Appointment> hrefs) throws IOException, DavException {
		ArrayList<Appointment> appts = new ArrayList<Appointment>();
		
		DavRequest multiget = DavRequest.CALENDARMULTIGET(url);
		multiget.addRequestProp(DavElements.E_GETETAG);
		multiget.addRequestProp(DavElements.E_CALENDAR_DATA);
		for (Appointment appt : hrefs)
			multiget.addHref(appt.href);
		Collection<DavObject> response = sendMultiResponseRequest(multiget);
		for (DavObject obj : response) {
			String href = obj.getHref();
			String etag = obj.getPropertyText(DavElements.E_GETETAG);
			String calData = obj.getPropertyText(DavElements.E_CALENDAR_DATA);
			if (href != null && calData != null)
				appts.add(new Appointment(href, etag, calData));
		}
		return appts;
	}
	
	public Collection<Appointment> getAllCalendarData(String url) throws IOException, DavException {
		ArrayList<Appointment> appts = new ArrayList<Appointment>();
		
		DavRequest query = DavRequest.CALENDARQUERY(url);
		query.addRequestProp(DavElements.E_GETETAG);
		query.addRequestProp(DavElements.E_CALENDAR_DATA);
		Collection<DavObject> response = sendMultiResponseRequest(query);
		for (DavObject obj : response) {
			String href = obj.getHref();
			String etag = obj.getPropertyText(DavElements.E_GETETAG);
			String calData = obj.getPropertyText(DavElements.E_CALENDAR_DATA);
			if (href != null && calData != null)
				appts.add(new Appointment(href, etag, calData));
		}
		return appts;
	}
	
	private HashSet<String> mCalendarHomeSet;
}
