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
package org.syncany.connection.plugins.bt;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.ClientListenerAdapter;
import com.turn.ttorrent.client.TorrentHandler;

/**
 * @author Christof Schulze <christof.schulze@gmx.net>
 * this was yanked from ReplicationCompletionListener by shevek
 */

public class ClientCompletionListener extends ClientListenerAdapter {

	private final Logger LOG = LoggerFactory.getLogger(ClientCompletionListener.class);
	private final CountDownLatch latch;
	private final TorrentHandler.State state;

	public ClientCompletionListener(CountDownLatch latch, TorrentHandler.State state) {
		this.latch = latch;
		this.state = state;
	}

	@Override
	public void clientStateChanged(Client client, Client.State state) {
		LOG.info("client=" + client + ", state=" + state);
	}

	@Override
	public void torrentStateChanged(Client client, TorrentHandler torrent, TorrentHandler.State state) {
		LOG.info("client=" + client + ", torrent=" + torrent + ", state=" + state);
		if (state == this.state)
			latch.countDown();
		/*
		 * switch (state) { case DONE: case SEEDING: try { client.stop(); } catch (Exception e) { throw Throwables.propagate(e); } break; }
		 */
	}
}