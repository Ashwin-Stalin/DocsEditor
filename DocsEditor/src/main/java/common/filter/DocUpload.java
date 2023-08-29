package common.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

public class DocUpload implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
		try(PrintWriter out = response.getWriter()){
			HttpServletRequest req = (HttpServletRequest) request;
			Boolean valid = true;
			for(Part part : req.getParts()) {
				if(part.getName().equals("docs") && (part.getSubmittedFileName().endsWith(".txt"))) {
					continue;
				}else {
					valid = false;
				}
			}
			if(valid) {
				chain.doFilter(request, response);
			}else {
				String scriptErrorTag = "<script>document.querySelector('#error-file').style=\"color: red;\";</script> ";
				request.setAttribute("error", scriptErrorTag);
				request.getRequestDispatcher("uploadDocs").include(request, response);
			}
		} catch (IOException e) {
            System.out.println("Catch IO Exception : " + e.getMessage());
        } catch (ServletException e) {
        	System.out.println("Catch Servlet Exception : " + e.getMessage());
        }
	}

	@Override
	public void init(FilterConfig fConfig) throws ServletException {
		
	}
	
	@Override
	public void destroy() {
		
	}

}
