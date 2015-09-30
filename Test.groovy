
/**
 * Created by abaskakau on 9/30/15.
 */

def script = new GroovyScriptEngine(this.binding.build.project.workspace.toString()).with {
    loadScriptByName('Library.groovy')
}

out("asfd")