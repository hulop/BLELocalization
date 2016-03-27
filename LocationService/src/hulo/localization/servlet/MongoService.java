package hulo.localization.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.types.ObjectId;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;

import hulo.localization.Sample;
import hulo.localization.data.DataUtils;

public class MongoService {
	private static final String MONGO_HOST = "localhost:27017", DB_NAME = "locationservicedb";
	private static MongoService sDS;

	private DB mDB = null;
	private GridFS mFS = null;

	public static MongoService getInstance() {
		if (sDS == null) {
			sDS = new MongoService(MONGO_HOST, DB_NAME);
		}
		return sDS;
	}

	public MongoService(String host, String dbName) {
		try {
			mFS = new GridFS(mDB = new MongoClient(host).getDB(dbName));
			System.out.println(JSON.serialize(getCollectionNames()));
			System.out.println(System.getProperty("user.dir"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	private Set<String> getCollectionNames() {
		return mDB != null ? mDB.getCollectionNames() : null;
	}

	public DBCollection getCollection(String collectionName) {
		return mDB != null && collectionName != null ? mDB.getCollection(collectionName) : null;
	}

	/*
	 * GridFS services
	 */
	public Object getFileList() {
		return mFS != null ? mFS.getFileList() : null;
	}

	public boolean isFileExists(String id) {
		return getFile(id) != null;
	}

	public GridFSDBFile getFile(String id) {
		return mFS != null ? mFS.findOne(id) : null;
	}
	
	private GridFSInputFile getNewFile() {
		return mFS != null ? mFS.createFile() : null;
	}
	
	public String saveFile(InputStream is, String contentType) throws IOException {
		GridFSInputFile file = getNewFile();	
		String id = file.getId().toString();
		OutputStream os = getFileOutputStream(id, contentType);
		if (os != null) {
			try {
				byte data[] = new byte[4096];
				int len = 0;
				while ((len = is.read(data, 0, data.length)) > 0) {
					os.write(data, 0, len);
				}
				return id;
			} finally {
				os.close();
			}
		}
		return null;
	}

	public boolean saveFile(String id, InputStream is, String contentType) throws IOException {
		OutputStream os = getFileOutputStream(id, contentType);
		if (os != null) {
			try {
				byte data[] = new byte[4096];
				int len = 0;
				while ((len = is.read(data, 0, data.length)) > 0) {
					os.write(data, 0, len);
				}
				return true;
			} finally {
				os.close();
			}
		}
		return false;
	}

	private OutputStream getFileOutputStream(String id, String contentType) {
		if (mFS != null) {
			try {
				deleteFile(id);
				GridFSInputFile dbFile = mFS.createFile(id);
				if (contentType != null) {
					dbFile.setContentType(contentType);
				}
				return dbFile.getOutputStream();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public boolean deleteFile(String id) {
		if (isFileExists(id)) {
			mFS.remove(id);
			return true;
		}
		return false;
	}

	private boolean readFile(GridFSDBFile dbFile, OutputStream os) throws IOException {
		InputStream is = null;
		try {
			is = dbFile.getInputStream();
			byte data[] = new byte[4096];
			int len = 0;
			while ((len = is.read(data, 0, data.length)) > 0) {
				os.write(data, 0, len);
			}
			return true;
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	/*
	 * http services
	 */
	public void sendFile(String path, HttpServletResponse response) throws IOException {
		GridFSDBFile file = getFile(path);
		if (file == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("File %s does not exist", path));
		} else {
			String contentType = file.getContentType();
			if (contentType != null) {
				response.setContentType(contentType);
			}
			OutputStream os = null;
			try {
				readFile(file, os = response.getOutputStream());
			} catch (Exception e) {
				System.err.println("Send error: " + path);
			} finally {
				if (os != null) {
					os.close();
				}
			}
		}
	}

	public void sendJSON(Object obj, HttpServletRequest request, HttpServletResponse response) throws IOException {
		boolean gzip = false;
		if (request != null) {
			String acceptedEncodings = request.getHeader("accept-encoding");
			gzip = acceptedEncodings != null && acceptedEncodings.indexOf("gzip") != -1;
		}
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");
		if (obj instanceof WriteResult) {
			String error = ((WriteResult) obj).getError();
			if (error != null) {
				obj = error;
			} else {
				obj = "OK";
			}
		}
		OutputStream os = null;
		try {
			byte data[] = JSON.serialize(obj).getBytes("UTF-8");
			os = response.getOutputStream();
			if (gzip && data.length >= 860) {
				response.setHeader("Content-Encoding", "gzip");
				GZIPOutputStream gzos = new GZIPOutputStream(os);
				gzos.write(data);
				gzos.finish();
				gzos.close();
			} else {
				os.write(data);
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} finally {
			if (os != null) {
				os.close();
			}
		}
	}

	public void sendCSV(Object obj, HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		boolean gzip = false;
		if (request != null) {
			String acceptedEncodings = request.getHeader("accept-encoding");
			gzip = acceptedEncodings != null && acceptedEncodings.indexOf("gzip") != -1;
		}
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/plain");
		
		if (obj instanceof WriteResult) {
			String error = ((WriteResult) obj).getError();
			if (error != null) {
				obj = error;
			} else {
				obj = "OK";
			}
		}
		
		OutputStream os = null;
		try {
			String json = JSON.serialize(obj);
			ByteArrayInputStream in = new ByteArrayInputStream(json.getBytes("utf-8"));
			List<Sample> samples = DataUtils.readJSONSamples(in);
			byte data[] = Sample.samplesToCSVString(samples).getBytes("UTF-8");
			
			os = response.getOutputStream();
			if (gzip && data.length >= 860) {
				response.setHeader("Content-Encoding", "gzip");
				GZIPOutputStream gzos = new GZIPOutputStream(os);
				gzos.write(data);
				gzos.finish();
				gzos.close();
			} else {
				os.write(data);
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} finally {
			if (os != null) {
				os.close();
			}
		}
	}
}
