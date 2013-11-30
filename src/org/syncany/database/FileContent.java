/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.database;

import java.util.Collection;

import org.syncany.database.mem.MemChunkEntry;
import org.syncany.database.mem.MemFileVersion;
import org.syncany.database.mem.MemMultiChunkEntry;
import org.syncany.database.mem.MemChunkEntry.ChunkEntryId;

/**
 * A file content represents the content of a file. It contains a list of 
 * references to {@link MemChunkEntry}s, and identifies a content by its checksum.
 *
 * <p>A file content is implicitly referenced by one or many {@link MemFileVersion}s
 * through the checksum attribute. A file content always contains the full list of
 * chunks it resembles. There are no deltas!
 * 
 * <p>Unlike the chunk list in a {@link MemMultiChunkEntry}, the order of the chunks
 * is very important, because a file can only be reconstructed if the order of
 * its chunks are followed.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public interface FileContent {
     
    public void addChunk(ChunkEntryId chunk);

    public byte[] getChecksum();

    public void setChecksum(byte[] checksum);

    public long getSize();

    public void setSize(long contentSize);

    public Collection<ChunkEntryId> getChunks();

}
