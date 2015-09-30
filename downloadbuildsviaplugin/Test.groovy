package downloadbuildsviaplugin

import hudson.model.*
import hudson.AbortException
import hudson.console.HyperlinkNote

/**
 * Created by abaskakau on 9/30/15.
 */

evaluate(new File(this.binding.build.project.workspace.toString()+"/downloadbuildsviaplugin/lib/Library.groovy"))

out("test")