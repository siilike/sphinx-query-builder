SphinxQL query builder
======================

A query builder for the SphinxQL of the [Sphinx search engine](http://sphinxsearch.com/). 

Usage
-----

    SphinxQueryBuilder b = new SphinxQueryBuilder("index");
    
    // SELECT *
    b.setSelectAllFields(true); // default
    
    // SELECT *, weight() AS w
    b.addField("weight() AS w"); // default
    
    // FROM index
    // WHERE MATCH(
    
    // @(name, description) ( Ma \-ry AND Jane)
    b.addQuery("name, description", SphinxQueryBuilder.QUERY_ESCAPE, Arrays.asList("Ma -ry", "Jane"));
    
    // @!(name) Ja\|ne
    b.addQuery("name", SphinxQueryBuilder.QUERY_ESCAPE | SphinxQueryBuilder.QUERY_IGNORE, Arrays.asList("Ja|ne"));

    // MAYBE @name Mary
    b.addQuery("name", SphinxQueryBuilder.QUERY_MAYBE, Arrays.asList("Mary"));
    // -- beware: | MAYBE is invalid

    // @description[50] Jane
    b.addQuery("description", 50, 0, Arrays.asList("Jane"));
    
    // ( a | c )
    NestedQueryBuilder n = new NestedQueryBuilder(SphinxQueryBuilder.QUERY_OR);
    
    // ( d AND e )
    NestedQueryBuilder a = n.stepIntoAND();
    a.add("name", 0, Arrays.asList("d", "e"));
    
    // ( x | y )
    NestedQueryBuilder c = n.stepIntoOR();
    a.add("name", 0, Arrays.asList("x", "y"));
    
    // ( @name ( d AND e ) | @name ( x | y ) )
    b.addQuery(n);
    
    // @name notsu
    b.addQuery("@name notsu");

    // )
    // -- end MATCH() in WHERE

    // AND w > 25
    b.addWhere("w > 25");
    
    // AND users IN(1, 2, 6, 9, 39, 20)
    b.addFilter("users", Arrays.asList(1, 2, 6, 9, 39, 20), false);
    
    // AND date BETWEEN 1402772398 AND 1402773398
    b.addFilterRange("date", 1402772398, 1402773398, false);
    
    // GROUP BY date
    b.setGroupBy("date");
    
    // WITHIN GROUP ORDER BY uid
    b.withinGroupOrderBy("uid");
    
    // ORDER BY w DESC, id ASC
    b.setSortMode("w DESC, id ASC"); // default
    
    // LIMIT 0, 100
    b.setLimits(0, 100);
    
    // OPTION boolean_simplify=1
    b.addOption("boolean_simplify", 1);
    
    // FACET company
    b.addFacet("company");

    // SELECT *, [...] FACET company
    String query = b.getQuery();
