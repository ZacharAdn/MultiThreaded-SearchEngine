import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import javax.swing.text.html.HTMLDocument.Iterator;

class DataBase {

	private final int limitCache;
	private Cache cache;
	private Task minZinCache, maxZinUpdate;
	private HashMap<Integer, Task> updateCache = null;
	private boolean firstIter = true;

	public DataBase(int Y, int M, Cache cache) {
		this.limitCache = M;
		this.cache = cache;
		this.updateCache = new HashMap<Integer, Task>(cache.getSize());

		minZinCache = new Task();
		maxZinUpdate = new Task();
		this.minZinCache.setZ(Integer.MAX_VALUE);
		this.maxZinUpdate.setZ(Integer.MIN_VALUE);

	}

	public Task readTask(Task data) throws IOException, InterruptedException {
		int fileName = data.getX() / 1000;
		File file = new File(fileName + ".DB");
		RandomAccessFile acFile = new RandomAccessFile(file, "rw");

		if (acFile.length() != 0) {
			int byteIndex = indexInByte(data.getX());

			if (byteIndex + 4 < acFile.length()) {
				acFile.seek(byteIndex);
				int y = acFile.readInt();
				int z = acFile.readInt();
				acFile.seek(0);
				if (z != 0) {// if y is already writed
					data.setY(y);
				data.setZ(z + 1);
				return data;
				}
			}
		}
		return data; //// else if we didnt find the Task we return null (-1) and we need to make new answer
	}

	public void writeTask(Task data) throws IOException, InterruptedException {

		int fileName = data.getX() / 1000;
		File file = new File(fileName + ".DB");
		RandomAccessFile acFile = new RandomAccessFile(file, "rw");

		int y = 0;
		int z = 0;
		int index = indexInByte(data.getX());
		int byt = Integer.SIZE / 8;
		if (index + byt * 2 < acFile.length()) {//the file already exist 

			acFile.seek(index);
			y = acFile.readInt();
			z = acFile.readInt();
			acFile.seek(0);
		}

		if (z != 0) {
			data.setZ(data.getZ() + z);
			acFile.seek(index + byt);
			acFile.writeInt(data.getZ());
			acFile.seek(0);
		} else {
			acFile.seek(0);
			acFile.seek(index);

			acFile.writeInt(data.getY());
			acFile.writeInt(data.getZ());

			acFile.seek(0);
			acFile.close();
		}
		putInUpdate(data);
	}

	private void putInUpdate(Task data) throws InterruptedException {
		if (updateCache.size() < cache.getSize()) {
			if (data.getZ() > limitCache) {
				if (firstIter) {// need to false after the first iteration
					if (data.getZ() < minZinCache.getZ()) {
						minZinCache = data;
					}
					updateCache.put(data.getX(), data);
				} else {// it is not the first time
					if (data.getZ() > minZinCache.getZ()) {
						if (data.getZ() > maxZinUpdate.getZ()) {
							maxZinUpdate = data;
						}
						updateCache.put(data.getX(), data);
					}
				}
			}
		}

		if (updateCache.size() == cache.getSize()) {
			if (firstIter) {
				firstIter = false;
			}
			minZinCache = cache.add(updateCache, minZinCache, maxZinUpdate);
			updateCache.clear();
			maxZinUpdate.setZ(Integer.MIN_VALUE);
		}
	}

	public int indexInByte(int x) {    //return match index in the file
		x = Math.abs(x);
		int Byte = Integer.SIZE / 8;
		int arr[] = new int[3];
		for (int i = 2; i > -1; i--) { /// change num from 1934 -> 934
			arr[i] = x % 10;
			x /= 10;
		}
		int ansIndex = arr[0] * 100 + arr[1] * 10 + arr[2];

		return ansIndex * Byte * 2;
	}





}
