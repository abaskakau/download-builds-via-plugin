package downloadbuildsviaplugin

import downloadbuildsviaplugin.lib.Library

/**
 * Created by abaskakau on 9/30/15.
 */

evaluate(new File(this.binding.build.project.workspace.toString()+"/downloadbuildsviaplugin/lib/Library.groovy"))

Library.out("test")