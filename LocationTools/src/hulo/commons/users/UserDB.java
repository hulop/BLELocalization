/*******************************************************************************
 * Copyright (c) 2014, 2023  IBM Corporation, Carnegie Mellon University and others
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/

package hulo.commons.users;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import hulo.localization.servlet.MongoService;

public class UserDB {
	private final static String COMMON_DBNAME = "lscommons", USER_COLNAME = "users", INFO_COLNAME = "dbinfo";
	private final MongoService commonDB;
	private final DBCollection userCol;
	private final DBCollection infoCol;

	public UserDB() {
		commonDB = MongoService.makeService(COMMON_DBNAME, true);
		userCol = commonDB.getCollection(USER_COLNAME);
		infoCol = commonDB.getCollection(INFO_COLNAME);
	}

	public JSONArray listUsers() throws JSONException {
		JSONArray users = new JSONArray();
		for (DBCursor cursor = userCol.find(); cursor.hasNext();) {
			users.add(new JSONObject(cursor.next().toString()));
		}
		return users;
	}

	public JSONObject findUser(String user) throws JSONException {
		DBObject obj = userCol.findOne(new BasicDBObject("_id", user));
		return obj != null ? new JSONObject(obj.toString()) : null;
	}

	public void updateUser(String json) {
		userCol.save((DBObject) com.mongodb.util.JSON.parse(json));
	}

	public void removeUser(String user) {
		userCol.remove(new BasicDBObject("_id", user));
	}

	public JSONObject findInfo(String db_name) throws JSONException {
		DBObject obj = infoCol.findOne(new BasicDBObject("_id", db_name));
		return obj != null ? new JSONObject(obj.toString()) : null;
	}

	public void updateInfo(String json) {
		infoCol.save((DBObject) com.mongodb.util.JSON.parse(json));
	}
}
