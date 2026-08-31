package lumi.insert.app.core.entity.nondatabase;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
 
import lumi.insert.app.dto.response.CustomerNameResponse;
import lumi.insert.app.dto.response.ProductName;
import lumi.insert.app.dto.response.SupplierNameResponse;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

/**
 ** Custom wrapper for class {@link Slice}. 
 *  <p>Used to optimize pagination by using last index parameter</p>.
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@NoArgsConstructor
@Setter
@Getter
public class SliceIndex<T> implements Serializable {
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
    private int numberOfElements;
    private int size;
    private int number;
    private boolean empty;
    private Object lastId;
    private List<T> content;

    public SliceIndex(Slice<T> slice) {
        this.first = slice.isFirst();
        this.last = slice.isLast();
        this.hasNext = slice.hasNext();
        this.hasPrevious = slice.hasPrevious();
        this.numberOfElements = slice.getNumberOfElements();
        this.size = slice.getSize();
        this.number = slice.getNumber();
        this.empty = slice.isEmpty();
        this.content = slice.getContent();

        if (!slice.isEmpty()) {
            T last = slice.getContent().getLast();

            if(last instanceof SupplierNameResponse){
                SupplierNameResponse response = (SupplierNameResponse) last;
                this.lastId = response.id();
            }

            if(last instanceof ProductName){
                ProductName response = (ProductName) last;
                this.lastId = response.id();
            }

            if(last instanceof CustomerNameResponse){
                CustomerNameResponse response = (CustomerNameResponse) last;
                this.lastId = response.id();
            }
        }
    }
}