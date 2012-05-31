/***************************************************************************************
 * Copyright (c) 2012 Norbert Nagold <norbert.nagold@gmail.com>                         *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.libanki.sync;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ichi2.libanki.Collection;

public class LocalServer extends BasicHttpSyncer implements HttpSyncer{
	Syncer syncer;
	
	public LocalServer(Collection col) {
		super(null, null);
		syncer = new Syncer(col, null);
	}
//
//	public JSONObject applyChanges(JSONObject kw) {
//		try {
//			return new JSONObject(kw.toString());
//		} catch (JSONException e) {
//			throw new RuntimeException(e);
//		}
//	}

	public HttpResponse meta() {
		JSONArray ja = syncer.meta();
		ja.put(0);
		HttpEntity ent;
		try {
			ent = new StringEntity(ja.toString());
			HttpResponse response = new BasicHttpResponse(new ProtocolVersion("", 1,1), 200, "");
			response.setEntity(ent);
			return response;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public JSONObject start(JSONObject o) {
		try {
			return syncer.start(o.getInt("minUsn"), o.getBoolean("lnewer"), o.getJSONObject("graves"));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public JSONObject applyChanges(JSONObject o) {
		try {
			return syncer.applyChanges(o.getJSONObject("changes"));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public JSONObject chunk() {
		return syncer.chunk();
	}

	public void applyChunk(JSONObject o) {
		syncer.applyChunk(o);
	}

	public JSONArray sanityCheck() {
		return syncer.sanityCheck();
	}
}
