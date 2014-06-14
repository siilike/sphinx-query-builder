package org.ketsu.sphinx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014, Lauri Keel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class SphinxQueryBuilder
{
	public static final Logger logger = LoggerFactory.getLogger(SphinxQueryBuilder.class);

	public static final int QUERY_NO_SPACES = 1; // replace spaces with underscores
	public static final int QUERY_ESCAPE = 2; // escape all characters with a meaning for Sphinx, except for AND, OR, etc
	public static final int QUERY_MAYBE = 4; // MAYBE
	public static final int QUERY_IGNORE = 8; // @!(...)
	public static final int QUERY_AND = 16; // mary AND jane
	public static final int QUERY_OR = 32; // mary OR jane

	public static final Pattern escape = Pattern.compile("([\\(\\)\\|\\-\\!\\@\\~\\\"\\&\\/\\^\\$\\=])");
	public static final Pattern quote = Pattern.compile("([\\\\'\0\n\r])");
	public static final Pattern trim = Pattern.compile("([\\!\\\"\\#\\$\\%\\&\\'\\(\\)\\*\\+\\,\\-\\.\\/\\:\\;\\<\\=\\>\\?\\@\\[\\\\\\]\\^\\_\\`\\{\\|\\}\\~\n\r\\x1a])");
	public static final Pattern eof = Pattern.compile("\\x1a");

	protected String index;
	protected List<String> fields;
	protected List<String> facets;
	protected boolean selectAllFields = true;
	protected String sort = "w DESC, id ASC";
	protected String groupBy;
	protected String withinGroupOrderBy;

	protected List<String> where;
	protected Map<String, String> options;

	protected StringBuilder query;

	protected int limitFrom;
	protected int limitTo;

	public SphinxQueryBuilder(String idx)
	{
		index = idx;
	}

	public SphinxQueryBuilder(SphinxQueryBuilder base)
	{
		index = base.index;
		selectAllFields = base.selectAllFields;
		sort = base.sort;
		groupBy = base.groupBy;
		withinGroupOrderBy = base.withinGroupOrderBy;
		limitFrom = base.limitFrom;
		limitTo = base.limitTo;

		if(base.fields != null)
		{
			fields = new ArrayList<String>(base.fields);
		}

		if(base.where != null)
		{
			where = new ArrayList<String>(base.where);
		}

		if(base.options != null)
		{
			options = new HashMap<String, String>(base.options);
		}

		if(base.query != null)
		{
			query = new StringBuilder(base.query);
		}

		if(base.facets != null)
		{
			facets = new ArrayList<String>(base.facets);
		}
	}

	public String getQuery()
	{
		StringBuilder q = new StringBuilder(50);

		q.append("SELECT ");

		if(selectAllFields)
		{
			q.append("*, ");
		}

		q.append("weight() AS w");

		if(fields != null && !fields.isEmpty())
		{
			for(String fi : fields)
			{
				q.append(", ");
				q.append(fi);
			}
		}

		q.append(" FROM ");
		q.append(index);

		if((where != null && !where.isEmpty()) || (query != null && query.length() != 0))
		{
			q.append(" WHERE ");

			if(query != null && query.length() != 0)
			{
				q.append("MATCH(");

				q.append(quote(query.toString()));

				q.append(")");
			}

			if(where != null && !where.isEmpty())
			{
				if(query != null && query.length() != 0)
				{
					q.append(" AND ");
				}

				Iterator<String> wi = where.iterator();
				while(wi.hasNext())
				{
					q.append(wi.next());

					if(wi.hasNext())
					{
						q.append(" AND ");
					}
				}
			}
		}

		if(groupBy != null)
		{
			q.append(" GROUP BY ");
			q.append(groupBy);
		}

		if(withinGroupOrderBy != null)
		{
			q.append(" WITHIN GROUP ORDER BY ");
			q.append(withinGroupOrderBy);
		}

		if(sort != null)
		{
			q.append(" ORDER BY ");
			q.append(sort);
		}

		q.append(" LIMIT ");
		q.append(limitFrom);
		q.append(",");
		q.append(limitTo);

		if(options != null && !options.isEmpty())
		{
			q.append(" OPTION ");

			Iterator<Map.Entry<String, String>> iter = options.entrySet().iterator();
			while(iter.hasNext())
			{
				Map.Entry<String, String> entry = iter.next();

				q.append(entry.getKey());
				q.append("=");
				q.append(entry.getValue());

				if(iter.hasNext())
				{
					q.append(", ");
				}
			}
		}

		if(facets != null)
		{
			Iterator<String> iter = facets.iterator();
			while(iter.hasNext())
			{
				String entry = iter.next();

				q.append("FACET ");
				q.append(entry);

				if(iter.hasNext())
				{
					q.append(" ");
				}
			}
		}

		String ret = q.toString();

		if(logger.isDebugEnabled())
		{
			logger.debug(ret);
		}

		return ret;
	}

	public SphinxQueryBuilder addOption(String key, Object val)
	{
		if(options == null)
		{
			options = new HashMap<String, String>();
		}

		options.put(key, String.valueOf(val));
		return this;
	}

	public SphinxQueryBuilder setGroupBy(String by)
	{
		groupBy = by;
		return this;
	}

	public SphinxQueryBuilder withinGroupOrderBy(String by)
	{
		withinGroupOrderBy = by;
		return this;
	}

	public SphinxQueryBuilder addField(String f)
	{
		if(fields == null)
		{
			fields = new ArrayList<String>();
		}

		fields.add(f);
		return this;
	}

	public SphinxQueryBuilder setSelectAllFields(boolean select)
	{
		selectAllFields = select;
		return this;
	}

	public SphinxQueryBuilder setSortMode(String sortMode)
	{
		sort = sortMode;
		return this;
	}

	public SphinxQueryBuilder addFilter(String field, Collection<? extends Number> val, boolean exclude)
	{
		if(!val.isEmpty())
		{
			StringBuilder b = new StringBuilder();

			b.append(field);
			b.append(" ");

			if(exclude)
			{
				b.append("NOT ");
			}

			b.append("IN(");

			Iterator iter = val.iterator();
			while(iter.hasNext())
			{
				b.append(iter.next());

				if(iter.hasNext())
				{
					b.append(",");
				}
			}

			b.append(")");

			addWhere(b.toString());
		}

		return this;
	}

	public SphinxQueryBuilder addFilter(String field, Number val, boolean exclude)
	{
		addWhere(field+(exclude ? "!=" : "=")+val);
		return this;
	}

	public SphinxQueryBuilder addFilterRange(String field, Number from, Number to, boolean exclude)
	{
		if(exclude)
		{
			throw new UnsupportedOperationException("NOT BETWEEN syntax is not supported by SphinxQL (yet)");
		}
		else
		{
			addWhere("("+field+" BETWEEN "+from+" AND "+to+")");
		}

		return this;
	}

	public SphinxQueryBuilder addWhere(String clause)
	{
		if(where == null)
		{
			where = new ArrayList<String>();
		}

		where.add(clause);
		return this;
	}

	public SphinxQueryBuilder addQuery(NestedQueryBuilder what)
	{
		if(what == null || what.isEmpty())
		{
			return this;
		}

		query = prepareQueryAppend(query, 0);

		what.appendTo(query);

		return this;
	}

	public SphinxQueryBuilder addQuery(String what, int options)
	{
		if(what == null || what.isEmpty())
		{
			return this;
		}

		query = prepareQueryAppend(query, options);

		query.append(what);

		return this;
	}

	public SphinxQueryBuilder addQuery(String what)
	{
		addQuery(what, 0);
		return this;
	}

	public SphinxQueryBuilder addQuery(String field, int limit, int options, Collection<?> val)
	{
		if(val == null || val.isEmpty())
		{
			return this;
		}

		query = prepareQueryAppend(query, options);

		buildQuery(query, field, limit, options & ~QUERY_MAYBE, val);

		return this;
	}

	public SphinxQueryBuilder addQuery(String field, int options, Collection<?> val)
	{
		addQuery(field, 0, options, val);
		return this;
	}

	public SphinxQueryBuilder addQuery(String field, int options, Object... val)
	{
		addQuery(field, options, Arrays.asList(val));
		return this;
	}

	public SphinxQueryBuilder addFacet(String facet)
	{
		if(facets == null)
		{
			facets = new ArrayList<String>();
		}

		facets.add(facet);
		return this;
	}

	public SphinxQueryBuilder addFacet(String field, String params)
	{
		addFacet(params == null ? field : field + " " + params);
		return this;
	}

	public SphinxQueryBuilder setLimits(int from, int to)
	{
		limitFrom = from;
		limitTo = to;
		return this;
	}

	public static StringBuilder prepareQueryAppend(StringBuilder query, int options)
	{
		if(query == null)
		{
			query = new StringBuilder(30);
		}
		else
		{
			query.append(" ");

			if((options & QUERY_OR) != 0)
			{
				query.append("| ");
			}
		}

		if((options & QUERY_MAYBE) != 0)
		{
			query.append("MAYBE ");
		}

		return query;
	}

	public static void buildQuery(StringBuilder query, String field, int limit, int options, Collection<?> val)
	{
		List<String> cur = new ArrayList<String>(val.size());

		for(Object v : val)
		{
			String x = processValue(v, options);

			if(!x.isEmpty())
			{
				cur.add(x);
			}
		}

		if(!cur.isEmpty())
		{
			if((options & QUERY_MAYBE) != 0)
			{
				query.append("MAYBE ");
			}

			query.append("@");

			if((options & QUERY_IGNORE) != 0)
			{
				query.append("!");
			}

			if(field.equals("*"))
			{
				query.append("*");
			}
			else
			{
				query.append("(");
				query.append(field);
				query.append(")");
			}

			if(limit != 0)
			{
				query.append("[");
				query.append(limit);
				query.append("]");
			}

			query.append(" (");

			Iterator<String> iter = cur.iterator();
			while(iter.hasNext())
			{
				query.append(iter.next());

				if(iter.hasNext())
				{
					query.append(" | ");
				}
			}

			query.append(")");
		}
	}

	public static String processValue(Object v, int options)
	{
		String x = String.valueOf(v).trim();

		if((options & QUERY_NO_SPACES) != 0)
		{
			x = x.replace(" ", "_");
		}

		if((options & QUERY_ESCAPE) != 0)
		{
			x = trimInvalid(x);
		}

		return x;
	}

	public static String escape(String what)
	{
		if(what == null)
		{
			return "";
		}

		return escape.matcher(what).replaceAll("\\\\$1");
	}

	public static String trimInvalid(String what)
	{
		if(what == null)
		{
			return "";
		}

		return trim.matcher(what).replaceAll("");
	}

	public static String quote(String what)
	{
		if(what == null)
		{
			return "''";
		}

		what = quote.matcher(what).replaceAll("\\\\$1");
		what = eof.matcher(what).replaceAll("\\Z");

		return "'"+what+"'";
	}

	public static class NestedQueryBuilder
	{
		protected NestedQueryBuilder parent;

		protected CharSequence query;
		protected boolean doQuote;

		protected List<NestedQueryBuilder> children;
		protected int options;

		public NestedQueryBuilder(NestedQueryBuilder parent, int options)
		{
			this.parent = parent;
			this.options = options;
		}

		public NestedQueryBuilder(NestedQueryBuilder parent, CharSequence query, boolean doQuote)
		{
			this.parent = parent;
			this.query = query;
			this.doQuote = doQuote;
		}

		public NestedQueryBuilder(int options)
		{
			this(null, options);
		}

		public NestedQueryBuilder()
		{
			this(null, 0);
		}

		public NestedQueryBuilder setOptions(int options)
		{
			this.options = options;
			return this;
		}

		public NestedQueryBuilder stepInto(int options)
		{
			NestedQueryBuilder ret = new NestedQueryBuilder(this, options);

			addChild(ret);

			return ret;
		}

		public NestedQueryBuilder stepInto()
		{
			return stepIntoAND();
		}

		public NestedQueryBuilder stepIntoAND()
		{
			return stepInto(QUERY_AND);
		}

		public NestedQueryBuilder stepIntoOR()
		{
			return stepInto(QUERY_OR);
		}

		public NestedQueryBuilder stepOut()
		{
			if(parent == null)
			{
				parent = new NestedQueryBuilder();
			}

			return parent;
		}

		public NestedQueryBuilder add(NestedQueryBuilder what)
		{
			if(what == null || what.isEmpty())
			{
				return this;
			}

			addChild(what);

			return this;
		}

		public NestedQueryBuilder add(String what, int options)
		{
			if(what == null || what.isEmpty())
			{
				return this;
			}

			StringBuilder query = prepareQueryAppend(null, options);

			query.append(what);

			addChild(new NestedQueryBuilder(this, query, true));

			return this;
		}

		public NestedQueryBuilder add(String what)
		{
			add(what, 0);
			return this;
		}

		public NestedQueryBuilder add(String field, int limit, int options, Collection<?> val)
		{
			if(val == null || val.isEmpty())
			{
				return this;
			}

			StringBuilder query = prepareQueryAppend(null, options);

			buildQuery(query, field, limit, options & ~QUERY_MAYBE, val);

			addChild(new NestedQueryBuilder(this, query, false));

			return this;
		}

		public NestedQueryBuilder add(String field, int options, Collection<?> val)
		{
			add(field, 0, options, val);
			return this;
		}

		public NestedQueryBuilder add(String field, int options, Object... val)
		{
			add(field, options, Arrays.asList(val));
			return this;
		}

		public boolean isEmpty()
		{
			if(query != null && query.length() != 0)
			{
				return false;
			}

			if(children == null)
			{
				return true;
			}

			for(NestedQueryBuilder c : children)
			{
				if(!c.isEmpty())
				{
					return false;
				}
			}

			return true;
		}

		protected void addChild(NestedQueryBuilder b)
		{
			if(children == null)
			{
				children = new ArrayList<NestedQueryBuilder>();
			}

			children.add(b);
		}

		public void appendTo(StringBuilder b)
		{
			if(query != null)
			{
				if(doQuote)
				{
					b.append("( ");
				}

				b.append(query);

				if(doQuote)
				{
					b.append(" )");
				}

				return;
			}

			if(children != null)
			{
				int total = children.size();

				if(total > 1)
				{
					b.append("( ");
				}

				String separator = " ";

				if((options & QUERY_OR) != 0)
				{
					separator = " | ";
				}
				else if((options & QUERY_MAYBE) != 0)
				{
					separator = " MAYBE ";
				}

				boolean hasElements = false;
				for(NestedQueryBuilder c : children)
				{
					if(!c.isEmpty())
					{
						c.appendTo(b);

						b.append(separator);

						hasElements = true;
					}
				}

				if(hasElements)
				{
					b.delete(b.length() - separator.length(), b.length());
				}

				if(total > 1)
				{
					b.append(" )");
				}
			}
		}

		@Override
		public String toString()
		{
			StringBuilder b = new StringBuilder();

			appendTo(b);

			return b.toString();
		}
	}
}
