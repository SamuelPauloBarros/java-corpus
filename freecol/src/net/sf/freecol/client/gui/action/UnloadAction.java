/**
 *  Copyright (C) 2002-2014   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;


/**
 * An action for unloading a unit.
 */
public class UnloadAction extends MapboardAction {

    public static final String id = "unloadAction";

    private Unit unit = null;


    /**
     * Creates an action for unloading the currently selected unit.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public UnloadAction(FreeColClient freeColClient) {
        this(freeColClient, null);
    }

    /**
     * Creates an action for unloading the <code>Unit</code>
     * provided.  If the <code>Unit</code> is <code>null</code>, then
     * the currently selected unit is used instead.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> to unload.
     * @see net.sf.freecol.client.gui.MapViewer#getActiveUnit()
     */
    public UnloadAction(FreeColClient freeColClient, Unit unit) {
        super(freeColClient, id);

        this.unit = unit;
    }


    private Unit getUnit() {
        return (unit != null) ? unit : getGUI().getActiveUnit();
    }

    /**
     * Checks if this action should be enabled.
     *
     * @return True if there is an active carrier with cargo to unload.
     */
    @Override
    protected boolean shouldBeEnabled() {
        final Unit carrier = getUnit();
        final Player player = freeColClient.getMyPlayer();
        return super.shouldBeEnabled()
            && carrier != null
            && carrier.isCarrier()
            && carrier.getCargoSpaceTaken() > 0
            && player != null && player.owns(carrier);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        Unit carrier = getUnit();
        if (carrier != null) igc().unload(carrier);
    }
}
