/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation
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
 *******************************************************************************/

package hulo.commons.filter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Servlet Filter implementation class UrlRewriteFilter
 */
@WebFilter("/*")
public class UrlRewriteFilter implements Filter {

	/**
	 * Default constructor.
	 */
	public UrlRewriteFilter() {
	}

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (UrlRewriter.process((HttpServletRequest) request, (HttpServletResponse) response, chain)) {
			return;
		}
		// pass the request along the filter chain
		chain.doFilter(request, response);
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		UrlRewriter.init(fConfig.getServletContext());
	}

	/*
	 * UrlRewriter
	 */
	private static class UrlRewriter {

		private static UrlRewriteRule[] rewriteRules = new UrlRewriteRule[0];
		private static int contextLength;

		public static void init(ServletContext context) {
			contextLength = context.getContextPath().length();
			InputStream is = context.getResourceAsStream("/WEB-INF/urlrewriterules.xml");
			if (is != null) {
				try {
					rewriteRules = UrlRewriteRule.loadRules(is);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			if (rewriteRules.length == 0) {
				System.err.println("No URL Rewrite Rules specified");
			}
		}

		public static boolean process(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
			String source = request.getRequestURI().substring(contextLength);
			for (int i = 0; i < rewriteRules.length; i++) {
				String target = rewriteRules[i].getTargetURI(source);
				if (target != null) {
					String query = request.getQueryString();
					if (query != null && (query = query.trim()).length() > 0) {
						target += (target.indexOf('?') > 0 ? "&" : "?") + query;
					}
					try {
						request.getRequestDispatcher(target).forward(request, response);
						return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			return false;
		}
	}

	/*
	 * UrlRewriteRule
	 */
	private static class UrlRewriteRule {

		private Pattern sourcePattern;
		private String targetString;

		public UrlRewriteRule(String fromString, String toString) {
			this.sourcePattern = Pattern.compile(fromString.trim());
			this.targetString = toString.trim();
		}

		public String getTargetURI(String source) {
			Matcher m = sourcePattern.matcher(source);
			return m.matches() ? m.replaceAll(targetString) : null;
		}

		public static UrlRewriteRule[] loadRules(InputStream is) throws Exception {
			List<UrlRewriteRule> ruleList = new ArrayList<UrlRewriteRule>();
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			NodeList ruleNodes = builder.parse(is).getElementsByTagName("rule");
			for (int i = 0; i < ruleNodes.getLength(); i++) {
				Element rule = (Element) ruleNodes.item(i);
				try {
					Node fromNode = rule.getElementsByTagName("from").item(0);
					Node toNode = rule.getElementsByTagName("to").item(0);
					ruleList.add(new UrlRewriteRule(fromNode.getTextContent(), toNode.getTextContent()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return ruleList.toArray(new UrlRewriteRule[ruleList.size()]);
		}
	}
}
