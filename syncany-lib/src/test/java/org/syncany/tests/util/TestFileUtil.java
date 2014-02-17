/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.tests.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.util.FileUtil;

/**
 * This class provides file I/O helper methods for writing tests
 * 
 * @author Philipp Heckel <philipp.heckel@gmail.com>
 * @author Nikolai Hellwig
 * @author Andreas Fenske
 */
public class TestFileUtil {	
	private static Random randomGen = new Random();
	private static Random nonRandomGen = new Random(123456789L); // fixed seed!
	
	private static File copyFileToDirectory(File from, File toDirectory) throws IOException {
		File outFile = new File(toDirectory, from.getName());
		
		InputStream in = new FileInputStream(from);
		OutputStream out = new FileOutputStream(outFile);

		byte[] buffer = new byte[1024];

		int length;
		// copy the file content in bytes
		while ((length = in.read(buffer)) > 0) {
			out.write(buffer, 0, length);
		}

		in.close();
		out.close();
		
		outFile.setLastModified(from.lastModified()); 	//Windows changes last modified when copying file
		
		return outFile;
	}

	/**
	 * 
	 * @param from Can be a folder or a file
	 * @param toDirectory Must be a folder
	 * @return 
	 * @throws IOException
	 */
	public static File copyIntoDirectory(File from, File toDirectory) throws IOException{
		if(from.isFile()){
			return copyFileToDirectory(from, toDirectory);
		}else{
			if(!toDirectory.exists()){
				toDirectory.mkdir();
			}
			
			for(File f: from.listFiles()){
				File srcFile = new File(from, f.getName());
				File destFile = new File(toDirectory, f.getName());

				copyIntoDirectory(srcFile, destFile);
			}
			
			return new File(toDirectory, from.getName());
		}
	}
	
	public static File createTempDirectoryInSystemTemp() throws Exception {
		return createTempDirectoryInSystemTemp("syncanytest");
	}
	
	public static File createTempDirectoryInSystemTemp(String prefix) throws Exception {
		File tempDirectoryInSystemTemp = new File(System.getProperty("java.io.tmpdir")+"/"+prefix);
		
		int i = 1;
		while (tempDirectoryInSystemTemp.exists()) {
			tempDirectoryInSystemTemp = new File(System.getProperty("java.io.tmpdir")+"/"+prefix+"-"+i);
			i++;
		}
		
		if (!tempDirectoryInSystemTemp.mkdir()) {
			throw new Exception("Cannot create temp. directory "+tempDirectoryInSystemTemp);
		}
		
		return tempDirectoryInSystemTemp;
	}

	public static boolean deleteDirectory(File path) {
		if (path!=null && path.exists() && path.isDirectory()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		} else return false;
		return (path.delete());
	}
	
	public static boolean deleteFile(File file) {
		if(file!=null && file.exists() && file.isFile()) {
			return file.delete();
		} else return false;
	}
	
	public static void changeRandomPartOfBinaryFile(File file) throws IOException {	
		if (file != null && !file.exists()) {
			throw new IOException("File does not exist: "+file);
		}

		if (file.isDirectory()) {
			throw new IOException("Cannot change directory: "+file);
		}

		// Prepare: random bytes at random position
		Random randomEngine = new Random();

		int fileSize = (int) file.length();
		int maxChangeBytesLen = 20;
		int maxChangeBytesStartPos = (fileSize-maxChangeBytesLen-1 >= 0) ? fileSize-maxChangeBytesLen-1 : 0;
		
		int changeBytesStartPos = (maxChangeBytesStartPos > 0) ? randomEngine.nextInt(maxChangeBytesStartPos) : 0;
		int changeBytesLen = (fileSize-changeBytesStartPos < maxChangeBytesLen) ? fileSize-changeBytesStartPos-1 : maxChangeBytesLen; 

		byte[] changeBytes = new byte[changeBytesLen];
		randomEngine.nextBytes(changeBytes);

		// Write to file
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

		randomAccessFile.seek(changeBytesStartPos);		
		randomAccessFile.write(changeBytes);
		
		randomAccessFile.close();
	}	
		
	public static File getRandomFilenameInDirectory(File rootFolder) {
		String fileName = "rndFile-" + System.currentTimeMillis() + "-" + Math.abs(randomGen.nextInt()) + ".dat";
		File newRandomFile = new File(rootFolder, fileName);
		
		return newRandomFile;
	}
	
	public static List<File> createRandomFileTreeInDirectory(File rootFolder, int maxFiles) throws IOException {
		List<File> randomFiles = new ArrayList<File>();
		List<File> randomDirs = new ArrayList<File>();
		File currentDir = rootFolder;
		
		for (int i=0; i<maxFiles; i++) {
			if (!randomDirs.isEmpty()) {
				currentDir = randomDirs.get((int) Math.random()*randomDirs.size());
			}
	
			if (Math.random() > 0.3) {
				File newFile = new File(currentDir+"/file"+i);
				int newFileSize = (int) Math.round(1000.0+Math.random()*500000.0);
				
				createRandomFile(newFile, newFileSize);				
				randomFiles.add(newFile);
			}
			else {
				currentDir = new File(currentDir+"/folder"+i);
				currentDir.mkdir();
				
				randomDirs.add(currentDir);
				randomFiles.add(currentDir);
			}
		}
		
		// Now copy some files (1:1 copy), and slightly change some of them (1:0.9) 
		for (int i=maxFiles; i<maxFiles+maxFiles/4; i++) {
			File srcFile = randomFiles.get((int) (Math.random()*(double)randomFiles.size()));
			File destDir = randomDirs.get((int) (Math.random()*(double)randomDirs.size()));			
			
			if (srcFile.isDirectory()) {
				continue;
			}
			
			// Alter some of the copies (change some bytes)
			if (Math.random() > 0.5) {
				File destFile = new File(destDir+"/file"+i+"-almost-the-same-as-"+srcFile.getName());
				FileUtils.copyFile(srcFile, destFile);
				
				changeRandomPartOfBinaryFile(destFile);				
				randomFiles.add(destFile);
			}
			
			// Or simply copy them
			else {
				File destFile = new File(destDir+"/file"+i+"-copy-of-"+srcFile.getName());
				FileUtils.copyFile(srcFile, destFile);
				
				randomFiles.add(destFile);
			}
		}
		
		return randomFiles;
	}	
	
	public static List<File> createRandomFilesInDirectory(File rootFolder, long sizeInBytes, int numOfFiles) throws IOException{
		List<File> newRandomFiles = new ArrayList<File>();
		
		for(int i = 0; i <numOfFiles; i++){
			newRandomFiles.add(createRandomFileInDirectory(rootFolder, sizeInBytes));
		}
		
		return newRandomFiles;
	}
	
	public static File createRandomFileInDirectory(File rootFolder, long sizeInBytes) throws IOException{		
		File newRandomFile = getRandomFilenameInDirectory(rootFolder);		
		createRandomFile(newRandomFile, sizeInBytes);
		
		return newRandomFile;
	}

	public static void createNonRandomFile(File fileToCreate, long sizeInBytes) throws IOException {
		createFile(fileToCreate, sizeInBytes, nonRandomGen);
	}
	
	public static void createRandomFile(File fileToCreate, long sizeInBytes) throws IOException {
		createFile(fileToCreate, sizeInBytes, randomGen);
	}
	
	private static void createFile(File fileToCreate, long sizeInBytes, Random randomGen) throws IOException {
		if (fileToCreate != null && fileToCreate.exists()){
			throw new IOException("File already exists");
		}
		
		FileOutputStream fos = new FileOutputStream(fileToCreate);
		int bufSize = 4096;
		long cycles = sizeInBytes / (long) bufSize;
		
		for(int i = 0; i < cycles; i++){
			byte[] randomByteArray = createArray(bufSize, randomGen);
			fos.write(randomByteArray);
		}
		
		// create last one
		// modulo cannot exceed integer range, so cast should be ok
		byte[] arr = createArray((int)(sizeInBytes % bufSize), randomGen);
		fos.write(arr);
		
		fos.close();
	}	
	
	public static void writeByteArrayToFile(byte[] inputByteArray, File fileToCreate) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileToCreate);		
		fos.write(inputByteArray);
		fos.close();			
	}	
	
	public static byte[] createArray(int size, Random randomGen) {
		byte[] ret = new byte[size];
		randomGen.nextBytes(ret);
		return ret;
	}		
	
	public static byte[] createRandomArray(int size) {
		return createArray(size, randomGen);
	}	

	public static byte[] createChecksum(File file) throws Exception {
		return FileUtil.createChecksum(file, "SHA1");
	}
	
	public static Map<String, File> getLocalFiles(File root) throws FileNotFoundException {
		return getLocalFiles(root, null);		
	}
	
	public static Map<String, File> getLocalFilesExcludeLockedAndNoRead(File root) throws FileNotFoundException {
		return getLocalFiles(root, new FileFilter() {
			@Override
			public boolean accept(File file) {
				return !FileUtil.isFileLocked(file) && FileUtil.canRead(file);
			}
		});		
	}
	
	public static Map<String, File> getLocalFiles(File root, FileFilter filter) throws FileNotFoundException {
		List<File> fileList = FileUtil.getRecursiveFileList(root, true, false);
		Map<String, File> fileMap = new HashMap<String, File>();
		
		for (File file : fileList) {
			if (filter != null && !filter.accept(file)) {
				continue;
			}
			
			String relativePath = FileUtil.getRelativePath(root, file);
			
			if (relativePath.startsWith(Config.DIR_APPLICATION)) {
				continue;
			}
			
			fileMap.put(relativePath, file);
		}
		
		return fileMap;		
	}
	
}
