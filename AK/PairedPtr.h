/*
 * Copyright (c) 2021, Arlen Keshabyan <arlen.albert@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace AK {

template <typename T>
struct HasValueType
{
    T value = {};

    HasValueType() = default;
    HasValueType(HasValueType &&) noexcept = default;
    HasValueType &operator= (HasValueType &&) noexcept = default;
};

struct NoValueType
{
    NoValueType() = default;
    NoValueType(NoValueType &&) noexcept = default;
    NoValueType &operator= (NoValueType &&) noexcept = default;
};

template <typename T1>
using BaseValueType = Conditional<IsSame<T1, Void<>>, NoValueType, HasValueType<T1>>;

template<typename T1 = Void<>, typename T2 = Void<>>
class PairedPtr : public BaseValueType<T1>
{
public:
	using BaseType = BaseValueType<T1>;
	using ConnectedPairedPtrType = PairedPtr<T2, T1>;

	template<typename, typename> friend class PairedPtr;
	template<typename T12, typename T22> friend bool operator==(const PairedPtr<T12, T22> &, const PairedPtr<T12, T22> &);
	template<typename T12, typename T22> friend bool operator!=(const PairedPtr<T12, T22> &, const PairedPtr<T12, T22> &);
	template<typename T12, typename T22> friend bool operator==(const PairedPtr<T12, T22> &, std::nullptr_t);
	template<typename T12, typename T22> friend bool operator!=(const PairedPtr<T12, T22> &, std::nullptr_t);
	template<typename T12, typename T22> friend bool operator==(std::nullptr_t, const PairedPtr<T12, T22> &);
	template<typename T12, typename T22> friend bool operator!=(std::nullptr_t, const PairedPtr<T12, T22> &);

	PairedPtr()
	{
	}

	PairedPtr(ConnectedPairedPtrType *ptr) : m_connected_paired_ptr{ ptr }
	{
		if (m_connected_paired_ptr) m_connected_paired_ptr->m_connected_paired_ptr = this;
	}

	PairedPtr(PairedPtr &&other) noexcept : PairedPtr{ other.m_connected_paired_ptr }
	{
		BaseType::operator=(static_cast<BaseType&&>(forward<PairedPtr>(other)));
		other.m_connected_paired_ptr = nullptr;
	}

	PairedPtr(const PairedPtr &other) = delete;
	PairedPtr & operator=(const PairedPtr &other) = delete;

	PairedPtr & operator=(PairedPtr &&other) noexcept
	{
		if (m_connected_paired_ptr)
		{
			if (other.m_connected_paired_ptr)
				swap(m_connected_paired_ptr->m_connected_paired_ptr, other.m_connected_paired_ptr->m_connected_paired_ptr);
			else
				m_connected_paired_ptr->m_connected_paired_ptr = &other;
		}
		else if (other.m_connected_paired_ptr)
			other.m_connected_paired_ptr->m_connected_paired_ptr = this;

		swap(m_connected_paired_ptr, other.m_connected_paired_ptr);

		BaseType::operator=(static_cast<BaseType&&>(forward<PairedPtr>(other)));

		return *this;
	}

	PairedPtr & operator=(ConnectedPairedPtrType *ptr)
	{
		return *this = PairedPtr(ptr);
	}

	~PairedPtr()
	{
		disconnect();
	}

	void disconnect()
	{
		if (m_connected_paired_ptr)
		{
			m_connected_paired_ptr->m_connected_paired_ptr = nullptr;
			m_connected_paired_ptr = nullptr;
		}
	}

	const ConnectedPairedPtrType * connected_ptr() const
	{
		return m_connected_paired_ptr;
	}

	operator bool() const
	{
        	return (m_connected_paired_ptr);
	}

	bool operator!() const
	{
        	return !(m_connected_paired_ptr);
	}

protected:
	ConnectedPairedPtrType *m_connected_paired_ptr = nullptr;

};

template<typename T1, typename T2>
bool operator==(const PairedPtr<T1, T2> & lhs, const PairedPtr<T1, T2> & rhs)
{
    return lhs.m_connected_paired_ptr == rhs.m_connected_paired_ptr;
}
template<typename T1, typename T2>
bool operator!=(const PairedPtr<T1, T2> & lhs, const PairedPtr<T1, T2> & rhs)
{
    return !(lhs == rhs);
}
template<typename T1, typename T2>
bool operator==(const PairedPtr<T1, T2> & lhs, std::nullptr_t)
{
    return !lhs;
}
template<typename T1, typename T2>
bool operator!=(const PairedPtr<T1, T2> & lhs, std::nullptr_t)
{
    return !(lhs == nullptr);
}
template<typename T1, typename T2>
bool operator==(std::nullptr_t, const PairedPtr<T1, T2> & rhs)
{
    return rhs == nullptr;
}
template<typename T1, typename T2>
bool operator!=(std::nullptr_t, const PairedPtr<T1, T2> & rhs)
{
    return !(nullptr == rhs);
}

}
