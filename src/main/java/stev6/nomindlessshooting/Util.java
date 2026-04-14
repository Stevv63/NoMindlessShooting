/*
   This file is part of NoMindlessShooting.
   NoMindlessShooting is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
   NoMindlessShooting is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
   You should have received a copy of the GNU General Public License along with NoMindlessShooting. If not, see <https://www.gnu.org/licenses/>.
*/

package stev6.nomindlessshooting;

import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

public class Util {
    private static final Pattern PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    public static Component parseMessage(String s) {
        return Component.literal(PATTERN.matcher(s).replaceAll("§$1"));
    }
}
