package eu.dkvz.BlogAuthoring.model;

import java.util.*;

public class Search {

  private List<String> include;

  public void cleanUpIncludes() {
    // Remove special chars and space
    // Check that there is still a value present that is more than 1
    // char long or remove the list item entirely.
    for (final ListIterator<String> i = this.include.listIterator(); i.hasNext();) {
      final String s = i.next().replaceAll("[+*$%\\s]", "");
      if (s.length() > 1) {
        i.set(s);
      } else {
        i.remove();
      }
    }
  }

  /**
   * @return the terms
   */
  public List<String> getInclude() {
    return include;
  }

  /**
   * @param terms the terms to set
   */
  public void setInclude(List<String> terms) {
    this.include = terms;
  }

  public String toQueryString() {
    return String.join(" ", this.include);
  }

}