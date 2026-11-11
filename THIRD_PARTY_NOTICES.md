# Third-Party Notices

This project is an unofficial fork of JustEnoughItems (JEI) for Minecraft.
It is distributed under the MIT License and the GNU Lesser General Public
License v3.0 or later. The complete source code is available at
https://github.com/StardustMINUS-01/JustEnoughItems.

This project is not affiliated with mezz or the GTNH Team.

## JustEnoughItems (JEI)

Copyright (c) 2014-2015 mezz
https://github.com/mezz/JustEnoughItems
Licensed under the MIT License. See LICENSES/MIT.txt.

## GTNewHorizons NotEnoughItems (adapted portions)

Copyright (c) 2019-2024 mitchej123 and the GTNH Team
https://github.com/GTNewHorizons/NotEnoughItems
Portions of this project are adapted from this software and are covered by
the GNU Lesser General Public License v3.0 or later.
See LICENSES/LGPL-3.0.txt and LICENSES/GPL-3.0.txt.

## NEI Features for JEI (adapted portions)

Copyright (c) 2024-2026
https://www.curseforge.com/minecraft/mc-mods/nei-features-for-jei
Portions of the crafting tree system (located in `mezz.jei.gui.tree`: `CraftingTreeScreen`, `TreeLayout`, `TreeCamera`, `TreeStats`, `TreeSlot`, `TreeGraph`) are ported and heavily adapted/refactored from NEI Features for JEI and GTNH NotEnoughItems, covered by the GNU Lesser General Public License v3.0 or later.
See LICENSES/LGPL-3.0.txt and LICENSES/GPL-3.0.txt.

## Additional modifications

Copyright (c) 2026 StardustMINUS and the Stellar Remnants Team, and Antigravity

Original additions are primarily located in:

- mezz.jei.gui.favorites and mezz.jei.gui.favorites.preferences (recipe tree generation and slot preference rules)
- mezz.jei.gui.compat.ae2, mezz.jei.gui.compat.gtm and the matching NeoForge compat packages (AE2 pattern encoding and GTM circuit integration)
- mezz.jei.gui.config.file.serializers and mezz.jei.common.config.file (JSON configuration storage)
- mezz.jei.gui.input (focused recipe resolution and input modifier handling)
- mezz.jei.gui.overlay.elements (placeholder and projected bookmark elements)
- mezz.jei.common.network.packets (crafting-grid and bookmark pull transport)
- mezz.jei.gui.bookmarks.chain.RecipeChainGraph, RecipeChainPlan and BookmarkPullPlanner (recipe chain graph and planning)
- mezz.jei.gui.tree (Crafting Tree system, BigInteger math calculation, manual recipe selector, manual quantity adjustment, and bidirectional bookmark chain conversion)

Note: mezz.jei.gui.bookmarks.chain.RecipeChainMath and mezz.jei.gui.tree contain portions adapted
from GTNH NEI and NEI Features for JEI (see the sections above), while its
graph-based chain computation, bidirectional conversion, BigInteger math, manual adjustments, and layout caching are original additions.
