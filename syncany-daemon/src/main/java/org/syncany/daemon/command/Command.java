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
package org.syncany.daemon.command;

import java.util.UUID;

import org.syncany.daemon.Daemon;
import org.syncany.daemon.DaemonEvent;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public abstract class Command {
	private CommandStatus status;
	private String id;
	
	public Command(){
		this.id = UUID.randomUUID().toString();
	}
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * @return the status
	 */
	public CommandStatus getStatus() {
		return status;
	}
	
	/**
	 * @param status the status to set
	 */
	public void setStatus(CommandStatus status) {
		this.status = status;
		Daemon.getEventBus().post(new DaemonEvent());
	}
	
	public abstract void disposeCommand();
}
