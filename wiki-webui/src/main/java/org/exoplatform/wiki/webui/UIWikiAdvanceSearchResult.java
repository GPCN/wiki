/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.webui;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.StringTokenizer;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.mow.core.api.wiki.AttachmentImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.RenamedMixin;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.webui.core.UIAdvancePageIterator;

@ComponentConfig(lifecycle = Lifecycle.class,
                 template = "app:/templates/wiki/webui/UIWikiAdvanceSearchResult.gtmpl",
                 events = {@EventConfig(listeners = UIWikiAdvanceSearchResult.ChangeMaxSizePageActionListener.class)})
public class UIWikiAdvanceSearchResult extends UIContainer {

  private PageList<SearchResult> results;

  public UIWikiAdvanceSearchResult() throws Exception {
    addChild(UIAdvancePageIterator.class, null, "SearchResultPageIterator");
  }

  public PageList<SearchResult> getResults() {
    return results;
  }
  
  public void setResults(PageList<SearchResult> results) {
    this.results = results;
    //pageIterator_.getPageList().setPageSize(5);
  }
  
  public int getItemsPerPage() {
    UIWikiPortlet wikiPortlet = getAncestorOfType(UIWikiPortlet.class);
    UIWikiAdvanceSearchForm advanceSearchForm = wikiPortlet.findFirstComponentOfType(UIWikiAdvanceSearchForm.class);
    return advanceSearchForm.getItemPerPage();
  }
  
  public String getKeyword() {
    UIWikiPortlet wikiPortlet = getAncestorOfType(UIWikiPortlet.class);
    UIWikiAdvanceSearchForm advanceSearchForm = wikiPortlet.findFirstComponentOfType(UIWikiAdvanceSearchForm.class);
    return advanceSearchForm.getKeyword();
  }

  protected String getDateFormat(Calendar cal) throws Exception {
    Locale currentLocale = Util.getPortalRequestContext().getLocale();
    DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, currentLocale);
    return df.format(cal.getTime());
  }
  
  protected PageImpl getPage(SearchResult result) {
    PageImpl page = null;
    try {
      if (WikiNodeType.WIKI_PAGE_CONTENT.equals(result.getType()) || WikiNodeType.WIKI_ATTACHMENT.equals(result.getType())) {
        AttachmentImpl searchContent = (AttachmentImpl) org.exoplatform.wiki.utils.Utils.getObject(result.getPath(), WikiNodeType.WIKI_ATTACHMENT);
        page = searchContent.getParentPage();
      } else if (WikiNodeType.WIKI_PAGE.equals(result.getType()) || WikiNodeType.WIKI_HOME.equals(result.getType())) {
        page = (PageImpl) org.exoplatform.wiki.utils.Utils.getObject(result.getPath(), WikiNodeType.WIKI_PAGE);
      }
      return page;
    } catch (Exception e) {
      return null;
    }
  }

  protected Wiki getWiki(PageImpl page) {
    return (page != null ? page.getWiki() : null);
  }

  protected String getOldPageTitleInSearchResult(PageImpl page, String pageTitle) throws Exception {
    UIWikiPortlet wikiPortlet = getAncestorOfType(UIWikiPortlet.class);
    UIWikiAdvanceSearchForm advanceSearchForm = wikiPortlet.findFirstComponentOfType(UIWikiAdvanceSearchForm.class);
    String keyword = advanceSearchForm.getKeyword();
    if (pageTitle.indexOf(keyword) >= 0) {
      return "";
    }
    if (page.getRenamedMixin() != null) {
      RenamedMixin mix = page.getRenamedMixin();
      for (String id : mix.getOldPageIds()) {
        if (id.indexOf(keyword) >= 0) {
          return replaceUnderscorebySpace(id);
        }
      }
    }
    return "";
  }
  
  protected static String replaceUnderscorebySpace(String s) {
    StringTokenizer st = new StringTokenizer(s, "_", false);
    StringBuilder sb = new StringBuilder();
    if (st.hasMoreElements()) {
      sb.append(st.nextElement());
    }
    while (st.hasMoreElements())
      sb.append(" ").append(st.nextElement());
    return sb.toString();
  }

  static public class ChangeMaxSizePageActionListener extends EventListener<UIWikiAdvanceSearchResult>{

    public void execute(Event<UIWikiAdvanceSearchResult> event) throws Exception {
      UIWikiAdvanceSearchResult uisearch = event.getSource();
      UIWikiPortlet wikiPortlet = uisearch.getAncestorOfType(UIWikiPortlet.class);
      UIWikiAdvanceSearchForm advanceSearchForm = wikiPortlet.findFirstComponentOfType(UIWikiAdvanceSearchForm.class);

      int itemsPerPage = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
      advanceSearchForm.setItemsPerPage(itemsPerPage);
      advanceSearchForm.gotoSearchPage(1);
      event.getRequestContext().addUIComponentToUpdateByAjax(uisearch);
    }
  }
}

