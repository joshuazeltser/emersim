/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.gui.jsimgraph.controller.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import jmt.gui.jsimgraph.controller.Mediator;

/**
 * <p>Title: Edit Simulation Parameters</p>
 * <p>Description: Action performed when pressing the Edit Simulation parameters... menu item.</p>
 *
 * @author Bertoli Marco
 *         Date: 20-set-2005
 *         Time: 13.29.00
 */
public class EditSimParams extends AbstractJmodelAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Defines an <code>Action</code> object with a default
	 * description string and default icon.
	 */
	public EditSimParams(Mediator mediator) {
		super("Simulation parameters...", "SimParam", mediator);
		putValue(SHORT_DESCRIPTION, "Define simulation parameters");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		setEnabled(false);
	}

	/**
	 * Invoked when an action occurs.
	 */
	public void actionPerformed(ActionEvent e) {
		// Edits
		mediator.editSimulationParameters();
	}
}