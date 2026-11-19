package io.github.itech_framework.code_editor.demo;

import io.github.itech_framework.code_editor.language.template.LanguageTemplate;
import io.github.itech_framework.code_editor.language.template.TemplateException;
import io.github.itech_framework.code_editor.language.template.TemplateLoader;

public final class TemplateCheck {

    public static void main(String[] args) {
        String valid = "{'version':1,'id':'demo','name':'Demo','extensions':['demo','.dm'],'states':{"
                + "'root':[{'match':'\\\\d+','scope':'number'},"
                + "{'words':['if','else'],'scope':'keyword'},"
                + "{'begin':'/\\\\*','end':'\\\\*/','scope':'comment.block'},"
                + "{'match':'#','scope':'string','push':'string'}],"
                + "'string':[{'match':'#','scope':'string','pop':true},{'eol':true,'pop':true}]}}";
        LanguageTemplate t = TemplateLoader.load("demo.json", valid.replace('\'', '"'));
        System.out.println(t.id() + " " + t.displayName() + " " + t.extensions() + " " + t.startState()
                + " " + t.states().keySet() + " " + t.states().get("root").size());

        System.out.println(t.states().get("string").get(1));
        
        fail("{'version':2,'id':'x','states':{'root':[]}}");
        fail("{'version':1,'id':'x','states':{'root':[{'match':'a','scope':'s','bogus':1}]}}");
        fail("{'version':1,'id':'x','states':{'root':[{'match':'(','scope':'s'}]}}");
        fail("{'version':1,'id':'x','states':{'root':[{'match':'a*','scope':'s'}]}}");
        fail("{'version':1,'id':'x','states':{'root':[{'match':'a','scope':'s','push':'nope'}]}}");
        fail("{'version':1,'id':'x','states':{'root':[{'match':'a'}]}}");
        fail("{'version':1,'states':{}}");
        fail("{'version':1,}");
        fail("{'version':1,'id':'x','states':{'root':[{'eol':true}]}}");
        fail("{'version':1,'id':'x','states':{'root':[{'eol':true,'scope':'s','pop':true}]}}");
        fail("{'version':1,'id':'x','states':{'root':[{'eol':false,'pop':true}]}}");
        fail("{'version':1,'id':'x','states':{'root':[{'eol':true,'match':'a','pop':true}]}}");
        fail("{'version':1,'id':'x','states':{'root':[{'eol':true,'push':'nope'}]}}");
    }

    private static void fail(String json) {
        try {
            TemplateLoader.load("t", json.replace('\'', '"'));
            System.out.println("NO ERROR");
        } catch (TemplateException ex) {
            System.out.println(ex.getMessage());
        }
    }
}