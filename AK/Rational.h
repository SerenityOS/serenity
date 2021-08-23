#pragma once

#include <AK/String.h>
#include <AK/Traits.h>
#include <AK/StringBuilder.h>
#include <string>

namespace AK {

template<typename T>
class Rational 
{
public:
	Rational() :
		m_numerator(0),
		m_denominator(1)
	{

	}

	Rational(T const numerator, T const denominator) 
		: 	m_numerator(numerator), 
			m_denominator(denominator)
	{
		assert(m_denominator != 0);
		static_assert(IsIntegral<T> == true);
	}

	T numerator() const 
	{
		return m_numerator;
	}

	T denominator() const 
	{
		return m_denominator;
	}

	double to_double() const
	{
		assert(m_denominator != 0);
		return static_cast<double>(m_numerator) / m_denominator;
	}

	String to_string() const
	{
		StringBuilder builder;
		// FIXME: Find an AK equivalent to std::to_string()
		builder.append(std::to_string(m_numerator).c_str());
		builder.append("/");
		builder.append(std::to_string(m_denominator).c_str());
		return builder.to_string();
	}


private:
	T m_numerator;
	T m_denominator;
};

}

using AK::Rational;