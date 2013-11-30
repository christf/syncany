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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.syncany.database.persistence.IDatabaseVersionHeader;

public class Branch {
	private ArrayList<IDatabaseVersionHeader> branch;
	
	public Branch() {
		this.branch = new ArrayList<IDatabaseVersionHeader>();
	}
	
	public void add(IDatabaseVersionHeader header) {
		branch.add(header);		
	}	
	
	public void addAll(List<IDatabaseVersionHeader> headers) {
		branch.addAll(headers);
	}	
	
	public int size() {
		return branch.size();
	}
	
	public IDatabaseVersionHeader get(int index) {
		try {
			return branch.get(index);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
	
	// TODO [medium] Performance: Use map instead of list
	public IDatabaseVersionHeader get(VectorClock vectorClock) {
		for (IDatabaseVersionHeader databaseVersionHeader : branch) {
			if (databaseVersionHeader.getVectorClock().equals(vectorClock)) {
				return databaseVersionHeader;
			}
		}
		
		return null;
	}

	public List<IDatabaseVersionHeader> getAll() {
		return Collections.unmodifiableList(branch);
	}	
	
	public IDatabaseVersionHeader getLast() {
		return branch.get(branch.size()-1);
	}	
	
	public BranchIterator iteratorLast() {
        return new BranchIterator(branch.size()-1);
    }
	
	public BranchIterator iteratorFirst() {
        return new BranchIterator(0);
    }	
	
	@Override
	public String toString() {
		return branch.toString();
	}
	
	public class BranchIterator implements Iterator<IDatabaseVersionHeader> {		
        private int current;
        
        public BranchIterator(int current) {
        	this.current = current;
        }
        
		@Override
		public boolean hasNext() {
			return current < branch.size();
		}
		
		public boolean hasPrevious() {
			return current > 0;
		}

		@Override
		public IDatabaseVersionHeader next() {
			return branch.get(current++);
		}
		
		public IDatabaseVersionHeader previous() {
			return branch.get(current--);
		}

		@Override
		public void remove() {
			throw new RuntimeException("Operation not supported, BranchIterator.remove()");			
		}
		
	}

}
