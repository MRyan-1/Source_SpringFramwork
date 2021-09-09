/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface that
 * reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 *
 * <p>The structure, elements, and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code <beans>} does not need to be the root
 * element of the XML document: this class will parse all bean definition elements
 * in the XML file, regardless of the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

	public static final String NESTED_BEANS_ELEMENT = "beans";

	public static final String ALIAS_ELEMENT = "alias";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String ALIAS_ATTRIBUTE = "alias";

	public static final String IMPORT_ELEMENT = "import";

	public static final String RESOURCE_ATTRIBUTE = "resource";

	//有了改属性，可以同时在配置文件中部署两套配置来适用于生产环境和开发环境，这样可以方便的进行切换开发，部署环境
	public static final String PROFILE_ATTRIBUTE = "profile";


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private XmlReaderContext readerContext;

	@Nullable
	private BeanDefinitionParserDelegate delegate;


	/**
	 * This implementation parses bean definitions according to the "spring-beans" XSD
	 * (or DTD, historically).
	 * <p>Opens a DOM Document; then initializes the default settings
	 * specified at the {@code <beans/>} level; then parses the contained bean definitions.
	 */
	@Override
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		this.readerContext = readerContext;
		//提取root，以便于再次将root作为参数继续完成BeanDefinition的注册
		//doRegisterBeanDefinitions 核心操作
		doRegisterBeanDefinitions(doc.getDocumentElement());
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 */
	protected final XmlReaderContext getReaderContext() {
		Assert.state(this.readerContext != null, "No XmlReaderContext available");
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor}
	 * to pull the source metadata from the supplied {@link Element}.
	 */
	@Nullable
	protected Object extractSource(Element ele) {
		return getReaderContext().extractSource(ele);
	}


	/**
	 * Register each bean definition within the given root {@code <beans/>} element.
	 */
	@SuppressWarnings("deprecation")  // for Environment.acceptsProfiles(String...)
	protected void doRegisterBeanDefinitions(Element root) {
		// Any nested <beans> elements will cause recursion in this method. In
		// order to propagate and preserve <beans> default-* attributes correctly,
		// keep track of the current (parent) delegate, which may be null. Create
		// the new (child) delegate with a reference to the parent for fallback purposes,
		// then ultimately reset this.delegate back to its original (parent) reference.
		// this behavior emulates a stack of delegates without actually necessitating one.
		BeanDefinitionParserDelegate parent = this.delegate;
		this.delegate = createDelegate(getReaderContext(), root, parent);

		if (this.delegate.isDefaultNamespace(root)) {
			//处理profile属性
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			if (StringUtils.hasText(profileSpec)) {
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
						profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				// We cannot use Profiles.of(...) since profile expressions are not supported
				// in XML config. See SPR-12458 for details.
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipped XML bean definition file due to specified profiles [" + profileSpec +
								"] not matching: " + getReaderContext().getResource());
					}
					return;
				}
			}
		}
		//面向对象设计方法学中常说的一句话，一个类要么是面向继承的设计的，要么就用final修饰。在DefaultBeanDefinitionDocumentReader中并没有用final修饰，所以它是面向继承而设计的。这两个方法正是为子类而设计的，可以很快速地反映出这是模版方法模式，如果继承自DefaultBeanDefinitionDocumentReader的子类需要在Bean解析前后做一些处理的话，那么只需要重写这两个方法就可以了。
		//解析前处理，留给子类实现
		preProcessXml(root);
		//解析并注册BeanDefinition
		parseBeanDefinitions(root, this.delegate);
		//解析后处理，留给子类实现
		postProcessXml(root);

		this.delegate = parent;
	}

	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {
		//Spring BeanDefinition的解析实在BeanDefinitionParserDelegate完成的
		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		delegate.initDefaults(root, parentDelegate);
		return delegate;
	}

	/**
	 * Parse the elements at the root level in the document:
	 * "import", "alias", "bean".
	 * 当Spring拿到一个元素时首先时根据命名空间进行解析，如果是默认的命名空间，则使用parseDefaultElement方法进行元素解析，如果使用的自定义声明配置那就使用parseCustom-Element方法进行解析
	 *
	 * @param root the DOM root element of the document
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		//对beans的处理
		//因为在Spring的XML配置里面有两大类Bean声明，一个是默认的，如： <bean id="test" class="test.TestBean" />
		//另一种就是自定义的 如： <tx:annotation-driven />
		//如何判断是否使用默认的Bean声明配置  可以通过判断是否使用默认的命名空间  如果命名空间是http://www.springframework.org/schema/beans 则是默认声明，否则是自定义声明
		if (delegate.isDefaultNamespace(root)) {
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element ele = (Element) node;
					if (delegate.isDefaultNamespace(ele)) {
						//对bean的处理 注册工作也在本方法内部实现
						parseDefaultElement(ele, delegate);
					} else {
						//对bean的处理
						delegate.parseCustomElement(ele);
					}
				}
			}
		} else {
			//处理Bean自定义声明配置
			delegate.parseCustomElement(root);
		}
	}

	/**
	 * 默认标签解析 注册工作也在本方法内部实现
	 *
	 * @param ele
	 * @param delegate
	 */
	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		//对import标签处理
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			importBeanDefinitionResource(ele);
		} else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			//对alias标签的处理
			processAliasRegistration(ele);
		} else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			//对bean标签的处理 注册工作也在本方法内部实现 look!
			processBeanDefinition(ele, delegate);
		} else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			//对beans标签的处理
			// recurse
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * 解析import标签
	 * 1. 获取resource属性所表示的路径
	 * 2. 解析路径中的系统属性，格式：${user.dir}
	 * 3. 判定location是绝对路径还是相对路径
	 * 4. 如果是绝对路径则地柜调用bean的解析过程，进行另一次的解析
	 * 5. 如果相对路径则计算出绝对路径进行解析
	 * 6. 通知监听器，解析完成
	 */
	protected void importBeanDefinitionResource(Element ele) {
		//获取Resource属性
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		//如果不存在resource属性则不作处理
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}
		//解析系统属性，格式"${user.dir}"
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<>(4);

		// 判定location是决定URL还是相对URL
		boolean absoluteLocation = false;
		try {
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		} catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// Absolute or relative?
		//如果是绝对URL 则直接根据地址加载对应的配置文件
		if (absoluteLocation) {
			try {
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		} else {
			//如果是相对地址则根据相对地址计算出绝对地址
			// No URL -> considering resource location as relative to the current file.
			try {
				int importCount;
				// Resource存在多个子实现类,如 VfsResource、FileSystemResource等 而每个 resource的createelative方式实现都不一样,所以这里先使用子类的方法尝试解析
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				if (relativeResource.exists()) {
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				} else {
					//如果解析不成功,则使用默认的解析器 ResourcePatternResolver进行解析
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			} catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from relative location [" + location + "]", ele, ex);
			}
		}
		//解析后进行监听器激活处理
		Resource[] actResArray = actualResources.toArray(new Resource[0]);
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * 对alias标签的处理
	 * <p>
	 * 在定义bean时就指定所有的别名并不是总是恰当的。有时我们期望能在当前位置为那些在别处定义的bean引入别名。在XML配置文件中,可用单独的< alias/>元素来完成 bean别名的定义。如配置文件中定义了一个 Javabean:
	 * <bean id="testbean" class=com.test"/>
	 * 要给这个Javabean増加別名,以方便不同对象来週用。我们就可以直接使用bean标签中的name属性:
	 * <bean id="testbean name="testBean,testbean2" class="com.test"/>
	 * 同样, Spring还有另外一种声明别名的方式:
	 * <bean id="testbean" class="com.test"/>
	 * <alias name="testbean" alias="testbean,testbean2"/>
	 */
	protected void processAliasRegistration(Element ele) {
		//获取beanName
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		//获取alias
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				//注册alias
				getReaderContext().getRegistry().registerAlias(name, alias);
			} catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			//别名注册后通知监听器做相应处理
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		//首先委托BeanDefinitionDelegate类的parseBeanDefinitionElement方法进行元素解析，返回BeanDefinitionHolder类型的实例bdHolder，经过这个方法后，bdHolder实例已经包含我们配置文件中配置的各种属性了，例如class、name、id、alias之类的属性
		//解析BeanDefinition
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		//当返回的bdHolder不为空的情况下若存在默认标签的子节点下再有自定义属性，还需要再次对自定义标签进行解析
		if (bdHolder != null) {
			//look! 适用场景<bean id="test" class="test.MyClass">  <mybean:user username="MRyan"/>    </bean> 使用了自定义配置
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				//解析完成之后，需要对解析后的bdHolder进行注册，同样，注册操作委托给了BeanDefinitionReaderUtils的registerBeanDefinition方法
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// 发出响应事件，通知相关的监听器，这个bean已经加载完成了  这里的实现只为了扩展，当开发人员需要注册BeanDefinition事件进行监听可以通过注册监听器的方式将处理逻辑写入监听器中，目前Spring没有对此事件做任何逻辑处理
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}
